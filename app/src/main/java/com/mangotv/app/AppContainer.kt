package com.mangotv.app

import android.content.Context
import com.mangotv.app.data.addon.AddonRepository
import com.mangotv.app.data.player.PlayerPreferencesRepository

/**
 * A small hand-rolled container instead of a DI framework: this app only has
 * a couple of app-scoped singletons so far, and Hilt/Dagger would be a lot
 * of ceremony for that.
 *
 * addonRepository is constructed eagerly, not lazily: its init block kicks
 * off restoring installed addons from disk back into ProviderRegistry, and
 * Home reads ProviderRegistry directly without ever touching this
 * repository. A lazy property would only construct (and start that
 * restore) the first time something visits Settings > Addons — meaning on
 * a fresh process (e.g. right after a device reboot) Home would show
 * "library is empty" indefinitely even with addons already installed,
 * since nothing ever re-registered them.
 *
 * playerPreferencesRepository has no such cross-screen dependency — only
 * the player itself ever reads it — so it stays lazy and is only
 * constructed (and only then restores its DataStore-backed record) the
 * first time playback actually starts.
 */
class AppContainer(context: Context) {
    val addonRepository: AddonRepository = AddonRepository(context)
    val playerPreferencesRepository: PlayerPreferencesRepository by lazy { PlayerPreferencesRepository(context) }
}
