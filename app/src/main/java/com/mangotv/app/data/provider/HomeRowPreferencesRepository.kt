package com.mangotv.app.data.provider

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.homeRowPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mango_home_row_prefs")

/**
 * Persists which Home catalog rows (keyed by HomeSection.id, which is
 * deterministic per catalog/genre — see StremioAddonProvider) the user has
 * chosen to hide from Settings > Home Rows.
 *
 * Stored as a hidden-set rather than a shown-set so a row nobody has an
 * opinion on yet — a brand new addon, or a newly added genre from an
 * existing one — defaults to visible instead of needing to be found and
 * explicitly re-enabled.
 */
class HomeRowPreferencesRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _hiddenRowIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenRowIds: StateFlow<Set<String>> = _hiddenRowIds.asStateFlow()

    init {
        scope.launch {
            _hiddenRowIds.value = appContext.homeRowPrefsDataStore.data.first()[HIDDEN_ROW_IDS_KEY].orEmpty()
        }
    }

    suspend fun setRowHidden(rowId: String, hidden: Boolean) = withContext(Dispatchers.IO) {
        val updated = if (hidden) _hiddenRowIds.value + rowId else _hiddenRowIds.value - rowId
        _hiddenRowIds.value = updated
        appContext.homeRowPrefsDataStore.edit { it[HIDDEN_ROW_IDS_KEY] = updated }
    }

    companion object {
        private val HIDDEN_ROW_IDS_KEY = stringSetPreferencesKey("hidden_row_ids")
    }
}
