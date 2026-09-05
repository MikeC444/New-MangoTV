package com.mangotv.app.navigation

object MangoRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SETTINGS_ADDONS = "settings/addons"
    const val SETTINGS_ADD_ADDON = "settings/addons/add"
}

/** Maps a top-nav label to the route it should navigate to, or null if that section isn't built yet. */
fun routeForNavLabel(label: String): String? = when (label) {
    "Home" -> MangoRoutes.HOME
    "Settings" -> MangoRoutes.SETTINGS
    else -> null
}
