package org.digma.intellij.plugin.codelens

import org.digma.intellij.plugin.codelens.providers.CodeLensMethodInsightsProvider1
import org.digma.intellij.plugin.codelens.providers.CodeLensMethodInsightsProvider10
import org.digma.intellij.plugin.codelens.providers.CodeLensMethodInsightsProvider2
import org.digma.intellij.plugin.codelens.providers.CodeLensMethodInsightsProvider3
import org.digma.intellij.plugin.codelens.providers.CodeLensMethodInsightsProvider4
import org.digma.intellij.plugin.codelens.providers.CodeLensMethodInsightsProvider5
import org.digma.intellij.plugin.codelens.providers.CodeLensMethodInsightsProvider6
import org.digma.intellij.plugin.codelens.providers.CodeLensMethodInsightsProvider7
import org.digma.intellij.plugin.codelens.providers.CodeLensMethodInsightsProvider8
import org.digma.intellij.plugin.codelens.providers.CodeLensMethodInsightsProvider9
import org.digma.intellij.plugin.codelens.providers.ErrorHotspotCodeLensProvider
import org.digma.intellij.plugin.codelens.providers.HighUsageCodeLensProvider
import org.digma.intellij.plugin.codelens.providers.LiveCodeLensProvider
import org.digma.intellij.plugin.codelens.providers.LowUsageCodeLensProvider
import org.digma.intellij.plugin.codelens.providers.ScaleFactorCodeLensProvider
import org.digma.intellij.plugin.codelens.providers.SlowEndpointCodeLensProvider

//don't convert to light service because it will register on all IDEs, but we want it only on Idea and Pycharm
@Suppress("LightServiceMigrationCode")
class CodeLensProviderFactory {
    private val genericProvidersList: MutableList<String> = ArrayList()

    init {
        initializeGenericProvidersMap()
    }

    fun getProviderId(lensId: String, usedGenericProviders: MutableList<String>): String {
        return with(lensId.uppercase()) {
            when {
                contains("LIVE") -> LiveCodeLensProvider.ID
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