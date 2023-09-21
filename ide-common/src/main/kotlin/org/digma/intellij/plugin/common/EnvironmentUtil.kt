package org.digma.intellij.plugin.common

const val LOCAL_ENV = "LOCAL"
const val LOCAL_TESTS_ENV = "LOCAL-TESTS"
const val SUFFIX_OF_LOCAL = "[LOCAL]"
const val SUFFIX_OF_LOCAL_TESTS = "[LOCAL-TESTS]"


fun isEnvironmentLocal(environment: String): Boolean {
    return environment.endsWith(SUFFIX_OF_LOCAL, true)
}

fun isEnvironmentLocalTests(environment: String): Boolean {
    return environment.endsWith(SUFFIX_OF_LOCAL_TESTS, true)
}

fun isLocalEnvironmentMine(environment: String, localHostname: String): Boolean {
    return environment.startsWith(localHostname, true)
}

fun buildEnvForLocalTests(localHostname: String): String {
    return localHostname + SUFFIX_OF_LOCAL_TESTS
}

fun buildEnvForLocalTests(): String {
    return buildEnvForLocalTests(CommonUtils.getLocalHostname())
}
