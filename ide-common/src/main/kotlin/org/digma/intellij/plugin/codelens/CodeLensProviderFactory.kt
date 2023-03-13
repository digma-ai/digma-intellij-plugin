package org.digma.intellij.plugin.codelens

import org.digma.intellij.plugin.codelens.providers.*

class CodeLensProviderFactory {
    private val genericProvidersList: MutableList<String> = ArrayList()

    init {
        initializeGenericProvidersMap()
    }

    fun getProviderId(lensTitle: String, usedGenericProviders: MutableList<String>): String {
        return with(lensTitle.uppercase()) {
            when {
                contains("ERROR") -> ErrorHotspotCodeLensProvider.ID
                contains("HIGH") -> HighUsageCodeLensProvider.ID
                contains("LOW") -> LowUsageCodeLensProvider.ID
                contains("SCALE") -> ScaleFactorCodeLensProvider.ID
                contains("SLOW") -> SlowEndpointCodeLensProvider.ID
                else -> getNotUsedGenericMethodInsightsProviderId(usedGenericProviders)
            }
        }
    }

    private fun getNotUsedGenericMethodInsightsProviderId(usedGenericProviders: MutableList<String>): String {
        val availableProvidersIds = genericProvidersList.filter { it !in usedGenericProviders }
        val availableProviderId = if (availableProvidersIds.isNotEmpty()) availableProvidersIds.first() else genericProvidersList.first()
        usedGenericProviders.add(availableProviderId)
        return availableProviderId
    }

    private fun initializeGenericProvidersMap() {
        genericProvidersList.add(CodeLensMethodInsightsProvider1.ID)
        genericProvidersList.add(CodeLensMethodInsightsProvider2.ID)
        genericProvidersList.add(CodeLensMethodInsightsProvider3.ID)
        genericProvidersList.add(CodeLensMethodInsightsProvider4.ID)
        genericProvidersList.add(CodeLensMethodInsightsProvider5.ID)
        genericProvidersList.add(CodeLensMethodInsightsProvider6.ID)
        genericProvidersList.add(CodeLensMethodInsightsProvider7.ID)
        genericProvidersList.add(CodeLensMethodInsightsProvider8.ID)
        genericProvidersList.add(CodeLensMethodInsightsProvider9.ID)
        genericProvidersList.add(CodeLensMethodInsightsProvider10.ID)
    }
}