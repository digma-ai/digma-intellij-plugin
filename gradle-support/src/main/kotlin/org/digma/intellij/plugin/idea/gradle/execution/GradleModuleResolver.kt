package org.digma.intellij.plugin.idea.gradle.execution

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.util.keyFMap.KeyFMap
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager

class GradleModuleResolver(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters) : ModuleResolver(configuration, params) {

    override fun findMainClassName(): String? {

        //casting must succeed or we have a bug
        val myConfiguration = configuration as GradleRunConfiguration

        val mainClass = if (containsGradleTestTask(myConfiguration.settings.taskNames)) {
            cleanupName(findClassNameFromTestConfiguration(myConfiguration))
        } else {
            cleanupName(findClassFromProgramConfiguration(myConfiguration))
        }

        if (mainClass != null) {
            return mainClass
        }

        //try super as last resort
        return super.findMainClassName()
    }


    private fun findClassNameFromTestConfiguration(configuration: GradleRunConfiguration): String? {

        //taskNames is actually the whole gradle command as in 'gradle :test --tests my.package.MyClass.myMethod'
        //and could be anything like 'gradle build myOtherTask myProject:test --tests my.package.MyClass.myMethod'
        //--tests is argument to the test task and must come after it.
        //we want to find the test name after --tests which may be a class or method name
        val tasks = configuration.settings.taskNames
        val hasTestTask = tasks.any {
            it.equals(":test") ||
                    it.endsWith(":test") ||
                    it.equals("test")
        }
        val dashTestsIndex = tasks.indexOf("--tests")
        if (hasTestTask && dashTestsIndex > -1 && tasks.size > dashTestsIndex) {
            val tests = tasks[dashTestsIndex + 1]
            if (tests != null) {
                return tests.trim('\"', '\'').trim()
            }
        }

        return null
    }


    private fun findClassFromProgramConfiguration(configuration: GradleRunConfiguration): String? {
        val confMap: KeyFMap = configuration.get()
        val gradleScriptContent = confMap.get(GradleTaskManager.INIT_SCRIPT_KEY)
        return gradleScriptContent?.let {
            findMainClassNameInGradleScript(it)
        }
    }


    private fun findMainClassNameInGradleScript(gradleScript: String): String? {

        // search for line like this:
        //     def mainClassToRun = 'ab.cde.AppMain'
        val lines = gradleScript.split("\n", "\r", "\r\n")

        val mainClassLine = lines.find { it.contains("mainClassToRun") }

        return mainClassLine?.let { line ->
            //todo: doesn't look logical
            val ixLast = line.lastIndexOf('\'')
            if (ixLast > -1) {
                val ixFirst = line.lastIndexOf('\'', ixLast - 1)
                if (ixFirst > -1) {
                    line.substring(ixFirst + 1, ixLast)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }


    private fun containsGradleTestTask(gradleTasks: Collection<String>): Boolean {
        return gradleTasks.any { isGradleTestTask(it) }
    }

    private fun isGradleTestTask(taskName: String): Boolean =
        taskName == ":test" ||
                taskName.endsWith(":test") ||
                taskName == "test"


    private fun cleanupName(className: String?): String? {
        return className?.replace('$', '.')
    }
}