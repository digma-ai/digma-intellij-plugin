package org.digma.intellij.plugin.idea.maven.execution

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.execution.services.BaseJvmRunConfigurationInstrumentationService
import org.jetbrains.idea.maven.execution.MavenRunConfiguration


//don't change to light service because it will register always. we want it to register only if gradle is enabled.
// see org.digma.intellij-with-gradle.xml
@Suppress("LightServiceMigrationCode")
class MavenRunConfigurationService : BaseJvmRunConfigurationInstrumentationService() {

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return configuration is MavenRunConfiguration
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean {

        return if (configuration is MavenRunConfiguration) {
            val goalNames = configuration.runnerParameters.goals
            val hasRelevantGoal = goalNames.any {
                it.equals("exec:exec") ||
                        it.equals("exec:java") ||
                        it.equals("-Dexec.executable=java") ||
                        it.equals("spring-boot:run") ||
                        (it.contains(":spring-boot-maven-plugin:") && it.endsWith(":run")) ||
                        (it.startsWith("tomcat") && it.endsWith(":run")) ||
                        (it.contains(":tomcat7-maven-plugin:") && it.endsWith(":run")) ||
                        (it.contains(":tomcat6-maven-plugin:") && it.endsWith(":run")) ||
                        (it.startsWith("jetty") && it.endsWith(":run")) ||
                        (it.contains(":jetty-maven-plugin:") && it.endsWith(":run"))
            }

            val isTest = isTest(configuration, params)
            return hasRelevantGoal || isTest
        } else {
            false
        }

    }


    override fun isTest(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters?): Boolean {
        return if (configuration is MavenRunConfiguration) {
            val goalNames = configuration.runnerParameters.goals
            val hasTestTask = goalNames.any {
                it.equals("surefire:test") ||
                        (it.contains(":maven-surefire-plugin:") && it.endsWith(":test")) ||
                        it.equals("spring-boot:test-run") ||
                        (it.contains(":spring-boot-maven-plugin:") && it.endsWith(":test-run"))
            }
            hasTestTask
        } else {
            return false
        }
    }


    override fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType {
        return if (configuration is MavenRunConfiguration) {
            if (isTest(configuration, params)) {
                RunConfigurationType.MavenTest
            } else {
                RunConfigurationType.MavenRun
            }
        } else {
            RunConfigurationType.Unknown
        }
    }

    override fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean {
        return configuration is MavenRunConfiguration
    }

    override fun getTaskNames(configuration: RunConfigurationBase<*>): Set<String> {
        if (configuration is MavenRunConfiguration) {
            return configuration.runnerParameters.goals.toSet()
        }
        return setOf()
    }

    override fun getBuildSystem(configuration: RunConfigurationBase<*>): BuildSystem {
        if (configuration is MavenRunConfiguration) {
            return BuildSystem.MAVEN
        }
        return BuildSystem.INTELLIJ
    }


}