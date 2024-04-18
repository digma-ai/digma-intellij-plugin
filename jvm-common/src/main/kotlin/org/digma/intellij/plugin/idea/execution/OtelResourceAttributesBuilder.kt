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

    private val otelResourceAttributes = mutableListOf<String>()


    open fun withCommonResourceAttributes(isTest: Boolean, parametersExtractor: ParametersExtractor): OtelResourceAttributesBuilder {

        if (needToAddDigmaEnvironmentAttribute(parametersExtractor)) {
            val envAttribute = if (isTest) {
                "$DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE=$LOCAL_TESTS_ENV"
            } else {
                "$DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE=$LOCAL_ENV"
            }

            withOtelResourceAttribute(envAttribute)
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
            val userIdAttribute = "$DIGMA_USER_ID_RESOURCE_ATTRIBUTE=${it}"
            withOtelResourceAttribute(userIdAttribute)
        }
        return this
    }

    open fun withScmCommitId(): OtelResourceAttributesBuilder {

        val commitId = VcsService.getInstance(configuration.project).getCommitIdForCurrentProject()
            ?: return this

        withOtelResourceAttribute("$DIGMA_SCM_COMMIT_ID_RESOURCE_ATTRIBUTE=$commitId")

        return this
    }


    private fun needToAddDigmaEnvironmentAttribute(parametersExtractor: ParametersExtractor): Boolean {
        return !parametersExtractor.hasDigmaEnvironmentIdAttribute(configuration, params) &&
                !parametersExtractor.hasDigmaEnvironmentAttribute(configuration, params)
    }


    open fun withOtelResourceAttribute(attribute: String): OtelResourceAttributesBuilder {
        //collecting otel resource attributes , they are built in the build method
        otelResourceAttributes.add(attribute)
        return this
    }


    open fun build(): String {
        return otelResourceAttributes.joinToString(",")
    }


}