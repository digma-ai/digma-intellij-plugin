package org.digma.intellij.plugin.analytics

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.rest.environment.Env

const val LOCAL_ENV = "LOCAL"
const val LOCAL_TESTS_ENV = "LOCAL-TESTS"
//const val SUFFIX_OF_LOCAL = "[LOCAL]"
//const val SUFFIX_OF_LOCAL_TESTS = "[LOCAL-TESTS]"


fun getAllEnvironments(project: Project): List<Env> {
    return AnalyticsService.getInstance(project).environment.environments
}

fun getAllEnvironmentsNames(project: Project): List<String> {
    return AnalyticsService.getInstance(project).environment.environments.map { it.name }
}

fun getCurrentEnvironment(project: Project): Env? {
    return AnalyticsService.getInstance(project).environment.current
}

fun getCurrentEnvironmentName(project: Project): String? {
    return AnalyticsService.getInstance(project).environment.current?.name
}

fun getCurrentEnvironmentId(project: Project): String? {
    return AnalyticsService.getInstance(project).environment.current?.id
}

fun getEnvironmentNameById(project: Project, envId: String): String? {
    return AnalyticsService.getInstance(project).environment.findById(envId)?.name
}

fun setCurrentEnvironmentById(project: Project, envId: String) {
    AnalyticsService.getInstance(project).environment.setCurrentById(envId)
}

fun setCurrentEnvironmentById(project: Project, envId: String, taskToRunAfterChange: Runnable) {
    AnalyticsService.getInstance(project).environment.setCurrentById(envId, taskToRunAfterChange)
}

fun refreshEnvironmentsNowOnBackground(project: Project) {
    AnalyticsService.getInstance(project).environment.refreshNowOnBackground()
}


//        fun getCurrentEnvId(project: Project): String? {
//            return AnalyticsService.getInstance(project).environment.getCurrent()?.id
//        }
//
//
//        fun getCurrentEnvName(project: Project): String? {
//            return AnalyticsService.getInstance(project).environment.getCurrent()?.name
//        }
//
//
//        fun getCurrentEnv(project: Project): Env? {
//            return AnalyticsService.getInstance(project).environment.getCurrent()
//        }
//
//
//        fun toEnv(env: String): Env {
//            val name = adjustEnvironmentDisplayName(env)
//            val type = if (isLocalEnvName(name)) {
//                "local"
//            } else {
//                "shared"
//            }
//            return Env(env, name, type)
//        }
//
//
//        fun filterRawEnvironments(environments: List<String>): List<String> {
//            val hostName = CommonUtils.getLocalHostname()
//            return environments.filter { env -> isNotLocal(env) || isMyLocalEnvironment(env, hostName) }
//        }
//
//
//        private fun isNotLocal(env: String): Boolean {
//            return !isEnvironmentLocal(env) && !isEnvironmentLocalTests(env)
//        }
//
//
//        fun isEnvironmentLocal(environment: String): Boolean {
//            return environment.endsWith(SUFFIX_OF_LOCAL, true)
//        }
//
//
//        fun isEnvironmentLocalTests(environment: String): Boolean {
//            return environment.endsWith(SUFFIX_OF_LOCAL_TESTS, true)
//        }
//
//
//        fun isLocalEnvironmentMine(environment: String, localHostname: String): Boolean {
//            return environment.startsWith(localHostname, true)
//        }
//
//
//        fun isMyLocalEnvironment(environment: String, localHostname: String): Boolean {
//            return (isEnvironmentLocal(environment) || isEnvironmentLocalTests(environment)) && environment.startsWith(localHostname, true)
//        }
//
//


//fun buildEnvForLocalTests(localHostname: String): String {
//    return localHostname + SUFFIX_OF_LOCAL_TESTS
//}
//
//
//fun buildEnvForLocalTests(): String {
//    return buildEnvForLocalTests(CommonUtils.getLocalHostname())
//}


//        fun adjustEnvironmentDisplayName(envName: String): String {
//
//            val hostname = CommonUtils.getLocalHostname()
//
//            return if (isEnvironmentLocal(envName) && isLocalEnvironmentMine(envName, hostname)) {
//                LOCAL_ENV
//            } else if (isEnvironmentLocalTests(envName) && isLocalEnvironmentMine(envName, hostname)) {
//                LOCAL_TESTS_ENV
//            } else {
//                envName
//            }
//        }

//
//        fun isLocalEnvName(env: String): Boolean {
//            return env == LOCAL_ENV || env == LOCAL_TESTS_ENV
//        }
//    }


//    @JsonIgnore
//    fun isLocal(): Boolean {
//        return isLocalEnv() || isLocalTestEnv()
//    }
//
//    private fun isLocalTestEnv(): Boolean {
//        return LOCAL_TESTS_ENV == name
//    }
//
//    private fun isLocalEnv(): Boolean {
//        return LOCAL_ENV == name
//    }
//}