package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.analytics.LOCAL_ENV
import org.digma.intellij.plugin.analytics.LOCAL_TESTS_ENV
import org.digma.intellij.plugin.analytics.isCentralized
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.vcs.VcsService

open class OtelResourceAttributesBuilder(
    private val configuration: RunConfiguration,
    private val params: SimpleProgramParameters,
    private val runnerSettings: RunnerSettings?
) {

    private val otelResourceAttributes = mutableMapOf<String, String>()


    open fun withCommonResourceAttributes(isTest: Boolean, parametersExtractor: ParametersExtractor): OtelResourceAttributesBuilder {

        if (needToAddDigmaEnvironmentAttribute(parametersExtractor)) {
            if (isTest) {
                withOtelResourceAttribute(DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE, LOCAL_TESTS_ENV)
            } else {
                withOtelResourceAttribute(DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE, LOCAL_ENV)
            }
        }

        if (!parametersExtractor.hasDigmaEnvironmentIdAttribute(configuration, params) &&
            isCentralized(configuration.project)
        ) {
            withUserId()
        }

        withScmCommitId()

        return this
    }


    open fun withUserId(): OtelResourceAttributesBuilder {
        DigmaDefaultAccountHolder.getInstance().account?.userId?.let {
            withOtelResourceAttribute(DIGMA_USER_ID_RESOURCE_ATTRIBUTE, it)
        }
        return this
    }

    open fun withScmCommitId(): OtelResourceAttributesBuilder {

        val commitId = VcsService.getInstance(configuration.project).getCommitIdForCurrentProject()
            ?: return this

        withOtelResourceAttribute(DIGMA_SCM_COMMIT_ID_RESOURCE_ATTRIBUTE, commitId)

        return this
    }


    private fun needToAddDigmaEnvironmentAttribute(parametersExtractor: ParametersExtractor): Boolean {
        return !parametersExtractor.hasDigmaEnvironmentIdAttribute(configuration, params) &&
                !parametersExtractor.hasDigmaEnvironmentAttribute(configuration, params)
    }


    open fun withOtelResourceAttribute(key: String, value: String): OtelResourceAttributesBuilder {
        //collecting otel resource attributes , they are built in the build method
        otelResourceAttributes[key] = value
        return this
    }


    open fun build(): Map<String, String> {
        return otelResourceAttributes
    }


}