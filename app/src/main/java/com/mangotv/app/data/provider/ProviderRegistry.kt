package com.mangotv.app.data.provider

/**
 * Runtime registry of installed catalog providers/addons. The Settings >
 * Addons screen (a later step) will read and mutate this registry; for now
 * it ships with a single built-in sample provider so the Home screen has
 * something real to render.
 */
object ProviderRegistry {
    private val providers = mutableListOf<CatalogProvider>(
        SampleCatalogProvider()
    )

    fun activeProviders(): List<CatalogProvider> = providers.toList()

    fun register(provider: CatalogProvider) {
        if (providers.none { it.id == provider.id }) {
            providers.add(provider)
        }
    }

    fun unregister(providerId: String) {
        providers.removeAll { it.id == providerId }
    }
}
