package org.digma.intellij.plugin.env

import com.fasterxml.jackson.annotation.JsonIgnore
import org.digma.intellij.plugin.common.CommonUtils

const val LOCAL_ENV = "LOCAL"
const val LOCAL_TESTS_ENV = "LOCAL-TESTS"
const val SUFFIX_OF_LOCAL = "[LOCAL]"
const val SUFFIX_OF_LOCAL_TESTS = "[LOCAL-TESTS]"


//must be data class, or a class with equals and hashCode
data class Env(
    val originalName: String,
    val name: String,
) {


    companion object {

        @JvmStatic
        fun toEnv(env: String): Env {
            return Env(env, adjustEnvironmentDisplayName(env))
        }

        @JvmStatic
        fun filterRawEnvironments(environments: List<String>): List<String> {
            val hostName = CommonUtils.getLocalHostname()
            return environments.filter { env -> isNotLocal(env) || isMyLocalEnvironment(env, hostName) }
        }

        @JvmStatic
        private fun isNotLocal(env: String): Boolean {
            return !isEnvironmentLocal(env) && !isEnvironmentLocalTests(env)
        }

        @JvmStatic
        fun isEnvironmentLocal(environment: String): Boolean {
            return environment.endsWith(SUFFIX_OF_LOCAL, true)
        }

        @JvmStatic
        fun isEnvironmentLocalTests(environment: String): Boolean {
            return environment.endsWith(SUFFIX_OF_LOCAL_TESTS, true)
        }

        @JvmStatic
        fun isLocalEnvironmentMine(environment: String, localHostname: String): Boolean {
            return environment.startsWith(localHostname, true)
        }

        @JvmStatic
        fun isMyLocalEnvironment(environment: String, localHostname: String): Boolean {
            return (isEnvironmentLocal(environment) || isEnvironmentLocalTests(environment)) && environment.startsWith(localHostname, true)
        }

        @JvmStatic
        fun buildEnvForLocalTests(localHostname: String): String {
            return localHostname + SUFFIX_OF_LOCAL_TESTS
        }

        @JvmStatic
        fun buildEnvForLocalTests(): String {
            return buildEnvForLocalTests(CommonUtils.getLocalHostname())
        }

        @JvmStatic
        fun adjustEnvironmentDisplayName(envName: String): String {

            val hostname = CommonUtils.getLocalHostname()

            return if (isEnvironmentLocal(envName) && isLocalEnvironmentMine(envName, hostname)) {
                LOCAL_ENV
            } else if (isEnvironmentLocalTests(envName) && isLocalEnvironmentMine(envName, hostname)) {
                LOCAL_TESTS_ENV
            } else {
                envName
            }
        }
    }


    @JsonIgnore
    fun isLocal(): Boolean {
        return isLocalEnv() || isLocalTestEnv()
    }

    private fun isLocalTestEnv(): Boolean {
        return LOCAL_TESTS_ENV == name
    }

    private fun isLocalEnv(): Boolean {
        return LOCAL_ENV == name
    }
}