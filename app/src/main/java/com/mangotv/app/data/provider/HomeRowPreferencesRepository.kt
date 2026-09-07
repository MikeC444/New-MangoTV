package com.mangotv.app.data.provider

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mangotv.app.data.model.HomeSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.homeRowPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mango_home_row_prefs")

/**
 * The user's manual row ordering plus which rows they've hidden, both keyed
 * by HomeSection.id (deterministic per catalog/genre — see
 * StremioAddonProvider), so preferences survive addon re-fetches across app
 * restarts.
 */
@Serializable
data class HomeRowPreferences(
    val order: List<String> = emptyList(),
    val hiddenRowIds: Set<String> = emptySet()
) {
    /**
     * Applies this manual order to a freshly-fetched section list: rows the
     * user has explicitly placed keep that relative order, and any row not
     * yet seen before (a brand new addon, or a newly added genre) is
     * appended at the end rather than needing to be found and re-sorted.
     * Does not filter hidden rows -- callers decide whether they want those
     * included (Home doesn't; the Home Rows settings list does, dimmed).
     */
    fun applyOrder(sections: List<HomeSection>): List<HomeSection> {
        val byId = sections.associateBy { it.id }
        val ordered = order.mapNotNull { byId[it] }
        val remaining = sections.filterNot { it.id in order }
        return ordered + remaining
    }
}

/** Persists [HomeRowPreferences] and exposes it as a StateFlow for Home and Settings > Home Rows. */
class HomeRowPreferencesRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _preferences = MutableStateFlow(HomeRowPreferences())
    val preferences: StateFlow<HomeRowPreferences> = _preferences.asStateFlow()

    init {
        scope.launch { _preferences.value = readPersisted() }
    }

    suspend fun setRowHidden(rowId: String, hidden: Boolean) = withContext(Dispatchers.IO) {
        val current = _preferences.value
        val updatedHidden = if (hidden) current.hiddenRowIds + rowId else current.hiddenRowIds - rowId
        update(current.copy(hiddenRowIds = updatedHidden))
    }

    /**
     * Reorders [rowId] by [delta] positions (-1 up, +1 down) within
     * [currentDisplayOrder] -- the full list of row ids exactly as Settings
     * is currently showing them, hidden ones included, so the persisted
     * order always reflects the last thing the user actually saw rather
     * than something recomputed independently of the screen state.
     */
    suspend fun moveRow(currentDisplayOrder: List<String>, rowId: String, delta: Int) = withContext(Dispatchers.IO) {
        val ids = currentDisplayOrder.toMutableList()
        val index = ids.indexOf(rowId)
        if (index < 0) return@withContext
        val newIndex = (index + delta).coerceIn(0, ids.lastIndex)
        if (newIndex == index) return@withContext
        ids.removeAt(index)
        ids.add(newIndex, rowId)
        update(_preferences.value.copy(order = ids))
    }

    private suspend fun update(new: HomeRowPreferences) {
        _preferences.value = new
        persist(new)
    }

    private suspend fun readPersisted(): HomeRowPreferences {
        val raw = appContext.homeRowPrefsDataStore.data.first()[PREFS_KEY] ?: return HomeRowPreferences()
        return runCatching { json.decodeFromString<HomeRowPreferences>(raw) }.getOrDefault(HomeRowPreferences())
    }

    private suspend fun persist(prefs: HomeRowPreferences) {
        val raw = json.encodeToString(prefs)
        appContext.homeRowPrefsDataStore.edit { it[PREFS_KEY] = raw }
    }

    companion object {
        private val PREFS_KEY = stringPreferencesKey("home_row_prefs_json")
    }
}
