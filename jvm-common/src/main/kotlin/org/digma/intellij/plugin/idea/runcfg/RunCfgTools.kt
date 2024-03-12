package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.jar.JarApplicationConfiguration
import com.intellij.execution.target.TargetEnvironmentsManager
import com.intellij.execution.wsl.target.WslTargetEnvironmentConfiguration
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.util.SystemInfo
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.keyFMap.KeyFMap
import org.digma.intellij.plugin.common.allowSlowOperation
import org.digma.intellij.plugin.common.runInReadAccessInSmartModeWithResultAndRetryIgnorePCE
import org.digma.intellij.plugin.log.Log
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import java.util.function.Supplier


private val logger: Logger = Logger.getInstance("org.digma.intellij.plugin.idea.runcfg.RunCfgTools")


fun <T : RunConfiguration?> tryResolveModule(
    configuration: T & Any, params: JavaParameters?,
): Module? {

    val runCfgFlavor = evalFlavor(configuration)
    if (runCfgFlavor == null) {
        Log.log(logger::warn, "could not find cfgFlavor for configuration class '{}'", configuration.javaClass)
        return null
    }

    val theModule = runCfgFlavor.tryResolveModule(params)
    return theModule
}


@VisibleForTesting
private fun evalFlavor(runCfg: RunConfiguration?): RunCfgFlavor<*>? {
    if (runCfg is GradleRunConfiguration) return GradleCfgFlavor(runCfg)
    if (runCfg is MavenRunConfiguration) return MavenCfgFlavor(runCfg)
    if (runCfg is JavaRunConfigurationBase) return JavaAppCfgFlavor(runCfg)
    if (runCfg is JarApplicationConfiguration) return JarAppCfgFlavor(runCfg)
    // no match, need to log warning
    return null
}


fun extractTasks(configuration: RunConfiguration): List<String> {
    return when (configuration) {
        is GradleRunConfiguration -> configuration.settings.taskNames
        is MavenRunConfiguration -> configuration.runnerParameters.goals
        else -> listOf()
    }
}

fun isWsl(configuration: RunConfigurationBase<*>): Boolean {
    if (!SystemInfo.isWindows)
        return false

    return isProjectUnderWsl(configuration) ||
            isConfigurationTargetWsl(configuration)
}


private fun isConfigurationTargetWsl(configuration: RunConfigurationBase<*>): Boolean {
    val targets = TargetEnvironmentsManager.getInstance(configuration.project).targets.resolvedConfigs()
    val targetName = (configuration.state as? RunConfigurationOptions)?.remoteTarget
    if (targetName == null)
        return false

    val target = targets.firstOrNull { it.displayName == targetName }
    if (target == null)
        return false

    if (target !is WslTargetEnvironmentConfiguration)
        return false

    return true
}

private fun isProjectUnderWsl(configuration: RunConfiguration): Boolean {
    return configuration.project.basePath?.startsWith("//wsl$/") == true ||
            configuration.project.basePath?.startsWith("//wsl.localhost/") == true
}


private abstract class RunCfgFlavor<T : RunConfiguration>(protected val runCfgBase: T) {

    val project: Project
        get() = runCfgBase.project

    val moduleManager: ModuleManager
        get() = ModuleManager.getInstance(project)

    val psiFacade: JavaPsiFacade
        get() = JavaPsiFacade.getInstance(project)

    val searchScope: GlobalSearchScope
        get() = GlobalSearchScope.projectScope(project)

    abstract fun evalMainClass(params: JavaParameters): String?

    open fun tryResolveModule(params: JavaParameters?): Module? {
        // first strategy, maybe module is just given
        if (runCfgBase is ModuleBasedConfiguration<*, *>) {
            val theCfgModule = runCfgBase.configurationModule
            theCfgModule?.let {
                return it.module
            }
        }

        if (params != null) {
            return tryResolveByJavaParameters(params)
        }
        return null
    }

    private fun tryResolveByJavaParameters(params: JavaParameters): Module? {
        // second strategy, maybe module is just given
        if (!params.moduleName.isNullOrBlank()) {
            val theModule = moduleManager.findModuleByName(params.moduleName)
            return theModule
        }

        // third strategy, by class name
        val mainClassFqn = evalMainClass(params)

        if (!mainClassFqn.isNullOrBlank()) {

            val module: Module? = allowSlowOperation(Supplier {

                val psiClass = runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
                    var cls: PsiClass? = psiFacade.findClass(mainClassFqn, searchScope)
                    if (cls == null) {
                        // try shorter name, since maybe the last part is method name
                        val shorterName = mainClassFqn.substringBeforeLast('.')
                        cls = psiFacade.findClass(shorterName, searchScope)
                    }
                    cls
                }

                if (psiClass != null) {
                    val theModule = ModuleUtilCore.findModuleForPsiElement(psiClass)
                    theModule
                } else {
                    null
                }
            })

            if (module != null) {
                return module
            }

        }

        // forth strategy, by working folder
        val workingFolder = params.workingDirectory

        if (!workingFolder.isNullOrBlank()) {
            val modules = moduleManager.modules
            val theModule = modules.firstOrNull {
                var result = false
                val guessedModuleDirVf = it.guessModuleDir()
                guessedModuleDirVf?.let {
                    val guessedDir = it.toNioPath().toString()
                    result = guessedDir == workingFolder
                }
                result
            }
            if (theModule != null) {
                return theModule
            }
        }

        // could not find the module
        return null
    }

}

private class GradleCfgFlavor(runCfgBase: GradleRunConfiguration) :
    RunCfgFlavor<GradleRunConfiguration>(runCfgBase) {

    override fun evalMainClass(params: JavaParameters): String? {
        return if (containsGradleTestTask(runCfgBase.settings.taskNames)) {
            cleanupName(evalClassFromConfigOfTest(params))
        } else {
            cleanupName(evalClassFromConfigOfMain(params))
        }
    }

    @VisibleForTesting
    private fun cleanupName(classFqn: String?): String? {
        if (classFqn == null) return null

        return classFqn
            // handle inner classes
            .replace('$', '.')
    }

    @VisibleForTesting
    private fun evalClassFromConfigOfMain(params: JavaParameters): String? {
        // this section relates to MAIN app run
        val confMap: KeyFMap = runCfgBase.get()
        val gradleScriptContent = confMap.get(GradleTaskManager.INIT_SCRIPT_KEY)
        return findMainClassName(gradleScriptContent)
    }

    @VisibleForTesting
    private fun containsGradleTestTask(gradleTasks: Collection<String>): Boolean {
        return gradleTasks.any {
            isGradleTestTask(it)
        }
    }

    @VisibleForTesting
    private fun isGradleTestTask(taskName: String): Boolean =
        taskName.endsWith(":test")

    @VisibleForTesting
    private fun findMainClassName(gradleScript: String?): String? {
        if (gradleScript.isNullOrBlank()) return null

        // search for line like this:
        //     def mainClassToRun = 'ab.cde.AppMain'
        val lines = gradleScript.split("\n", "\r", "\r\n")

        val relevantLine = lines.firstOrNull {
            it.contains("mainClassToRun")
        }
        if (relevantLine != null) {
            val ixLast = relevantLine.lastIndexOf('\'')
            if (ixLast > -1) {
                val ixFirst = relevantLine.lastIndexOf('\'', ixLast - 1)
                if (ixFirst > -1) {
                    val classFqn = relevantLine.substring(ixFirst + 1, ixLast)
                    return classFqn
                }
            }
        }

        return null
    }

    @VisibleForTesting
    private fun evalClassFromConfigOfTest(params: JavaParameters): String? {
//        myTaskNames = {ArrayList@60614}  size = 3
//        0 = ":test"
//        1 = "--tests"
//        2 = ""org.springframework.samples.petclinic.model.ValidatorTests""
        var foundTestTask = false
        var extractedClassNameValue: String? = null
        for (currTask in runCfgBase.settings.taskNames) {
            if (isGradleTestTask(currTask)) {
                foundTestTask = true
                continue
            }
            if (currTask.startsWith("--")) {
                continue
            }
            if (foundTestTask) {
                extractedClassNameValue = currTask.trim('\"', '\'').trim()
                break
            }
        }
        return extractedClassNameValue
    }

}

private class MavenCfgFlavor(runCfgBase: MavenRunConfiguration) :
    RunCfgFlavor<MavenRunConfiguration>(runCfgBase) {

    override fun evalMainClass(params: JavaParameters): String? = null

}

private class JavaAppCfgFlavor(runCfgBase: JavaRunConfigurationBase) :
    RunCfgFlavor<JavaRunConfigurationBase>(runCfgBase) {

    override fun evalMainClass(params: JavaParameters): String? = params.mainClass

}

private class JarAppCfgFlavor(runCfgBase: JarApplicationConfiguration) :
    RunCfgFlavor<JarApplicationConfiguration>(runCfgBase) {

    override fun evalMainClass(params: JavaParameters): String? = params.mainClass

    override fun tryResolveModule(params: JavaParameters?): Module? {
        return runCfgBase.module
    }
}
