package com.mangotv.app

import android.content.Context
import com.mangotv.app.data.addon.AddonRepository

/**
 * A small hand-rolled container instead of a DI framework: this app only has
 * a couple of app-scoped singletons so far, and Hilt/Dagger would be a lot
 * of ceremony for that.
 */
class AppContainer(context: Context) {
    val addonRepository: AddonRepository by lazy { AddonRepository(context) }
}
