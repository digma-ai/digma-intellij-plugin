package org.digma.intellij.plugin.common

const val LOCAL_ENV = "LOCAL"
const val LOCAL_TESTS_ENV = "LOCAL-TESTS"
const val SUFFIX_OF_LOCAL = "[LOCAL]"
const val SUFFIX_OF_LOCAL_TESTS = "[LOCAL-TESTS]"

fun getSortedEnvironments(
    envsList: List<String>,
    localHostname: String,
): List<String> {
    val builtEnvs = ArrayList<String>()

    var mineLocalEnv = ""
    var mineLocalTestsEnv = ""

    for (currEnv in envsList) {
        if (isEnvironmentLocal(currEnv)) {
            if (isLocalEnvironmentMine(currEnv, localHostname)) {
                mineLocalEnv = LOCAL_ENV
            } else {
                // skip - its other local (not mine)
                continue
            }
        } else if (isEnvironmentLocalTests(currEnv)) {
            if (isLocalEnvironmentMine(currEnv, localHostname)) {
                mineLocalTestsEnv = LOCAL_TESTS_ENV
            } else {
                // skip - its other local-tests (not mine)
                continue
            }
        } else {
            builtEnvs.add(currEnv)
        }
    }

    builtEnvs.sortWith(String.CASE_INSENSITIVE_ORDER)
    if (mineLocalTestsEnv.isNotBlank()) {
        builtEnvs.add(0, mineLocalTestsEnv)
    }
    if (mineLocalEnv.isNotBlank()) {
        builtEnvs.add(0, mineLocalEnv)
    }

    return builtEnvs
}

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
