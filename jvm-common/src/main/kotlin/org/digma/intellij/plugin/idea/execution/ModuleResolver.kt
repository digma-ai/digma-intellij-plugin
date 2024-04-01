package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.guessModuleDir
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.digma.intellij.plugin.common.allowSlowOperation
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import java.util.function.Supplier

open class ModuleResolver(
    protected val configuration: RunConfigurationBase<*>,
    protected val params: SimpleProgramParameters
) {


    open fun findMainClassName(): String? {
        if (configuration is CommonJavaRunConfigurationParameters &&
            configuration.runClass != null
        ) {
            return configuration.runClass
        }
        if (params is SimpleJavaParameters && params.mainClass != null) {
            return params.mainClass
        }
        return null
    }


    open fun resolveModule(): Module? {

        if (configuration is ModuleBasedConfiguration<*, *> &&
            configuration.configurationModule != null &&
            configuration.configurationModule.module != null
        ) {
            return configuration.configurationModule.module
        }

        if (params is SimpleJavaParameters && params.moduleName != null) {
            val module = findByModuleName(params.moduleName)
            if (module != null) {
                return module
            }
        }

        run {
            val moduleByClassName = findByClassName(configuration, params)
            if (moduleByClassName != null) {
                return moduleByClassName
            }
        }

        run {
            val moduleByWorkingDirectory = findByWorkingDirectory(configuration, params)
            if (moduleByWorkingDirectory != null) {
                return moduleByWorkingDirectory
            }
        }


        return null

    }


    open fun findByClassName(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Module? {
        val mainClassName = findMainClassName()
        return mainClassName?.let { className ->

            allowSlowOperation(Supplier {
                val psiClass = runInReadAccessWithResult {
                    var psiCls =
                        JavaPsiFacade.getInstance(configuration.project).findClass(className, GlobalSearchScope.projectScope(configuration.project))
                    if (psiCls == null) {
                        //some module resolvers may return a class name that contains the method name. mainly GradleModuleResolver
                        // will return my.package.MyClass.myMethod if it's a test run of a method.
                        // so if the psi class is not found try again without the method part
                        val classNameWithoutMethod = className.substringBeforeLast(".")
                        psiCls = JavaPsiFacade.getInstance(configuration.project)
                            .findClass(classNameWithoutMethod, GlobalSearchScope.projectScope(configuration.project))
                    }
                    psiCls
                }

                psiClass?.let {
                    ModuleUtilCore.findModuleForPsiElement(it)
                }
            })
        }
    }


    open fun findByWorkingDirectory(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Module? {
        val workingDirectory = params.workingDirectory

        return workingDirectory?.let { workDir ->
            val modules = ModuleManager.getInstance(configuration.project).modules
            modules.find {
                it.guessModuleDir()?.toNioPath().toString() == workDir
            }
        }
    }


    private fun findByModuleName(moduleName: String): Module? {
        return ModuleManager.getInstance(configuration.project).findModuleByName(moduleName)
    }

}