package org.digma.intellij.plugin.codelens

import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.ConcurrentHashMap
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_HIGH_TRY_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.model.lens.CodeLens

private const val NUMBER_OF_PROVIDERS = 30

//don't convert to light service because it will register on all IDEs, but we want it only on Idea and Pycharm
@Suppress("LightServiceMigrationCode")
class CodeVisionProviderToLensSelector(private val project: Project) {

    private val providerToLensIds = ConcurrentHashMap(mutableMapOf<String, String>())

    //should be very fast, it runs in read access
    fun selectLensForProvider(providerId: String, codeLenses: Set<CodeLens>): String? {

        //all providers already assigned a lens id, that means we have more than 30 code lens types.
        // report an error, if we see this error than we know we need more than 30 providers.
        // in that case just register more providers in CodeVisionProviders.kt and in org.digma.intellij-with-codevision.xml.
        if (providerToLensIds.size >= NUMBER_OF_PROVIDERS) {

            //todo: maybe execute on background to free up this code quickly. although reportError should be very fast
            ErrorReporter.getInstance().reportError(
                project, "CodeVisionProviderToLensSelector.selectLensForProvider",
                "selectLensForProvider,not enough code vision providers",
                mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_HIGH_TRY_FIX,
                    "error hint" to "not enough code vision providers"
                )
            )
            return null
        }


        return providerToLensIds[providerId] ?: selectUnassigned(providerId, codeLenses)
    }

    //this method is synchronized to prevent two providers from assigning the same lens id.
    // it is executed once per provider per project because if the provider was already assigned an id
    // it will not be called.
    @Synchronized
    private fun selectUnassigned(providerId: String, codeLenses: Set<CodeLens>): String? {
        val lensId: String? = if (providerId == LIVE_PROVIDER_ID) {
            codeLenses.find { lens: CodeLens -> isLiveLens(lens) }?.id
        } else {
            //this will return null if all provider already were assigned a lens id.
            //this code will actually not be called if all providers are assigned a lens id
            // because there is a check if providerToLensIds.size >= NUMBER_OF_PROVIDERS
            codeLenses.firstOrNull { lens: CodeLens -> !isLiveLens(lens) && !isAlreadyAssigned(lens) }?.id
        }

        lensId?.also {
            providerToLensIds[providerId] = it
        }

        return lensId
    }


    private fun isAlreadyAssigned(lens: CodeLens): Boolean {
        return providerToLensIds.values.contains(lens.id)
    }


    private fun isLiveLens(lens: CodeLens): Boolean {
        return lens.id.uppercase().contains("LIVE")
    }

}