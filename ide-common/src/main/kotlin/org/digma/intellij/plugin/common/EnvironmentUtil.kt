package org.digma.intellij.plugin.common

const val LOCAL_ENV = "LOCAL"
const val SUFFIX_OF_LOCAL = "[LOCAL]"

fun getSortedEnvironments(
        envsList: List<String>,
        localHostname: String
): List<String> {
    val builtEnvs = ArrayList<String>()

    var mineLocalEnv = ""

    for (currEnv in envsList) {
        if (isEnvironmentLocal(currEnv)) {
            if (isLocalEnvironmentMine(currEnv, localHostname)) {
                mineLocalEnv = LOCAL_ENV
            } else {
                // skip other local (not mine)
            }
            continue
        } else {
            builtEnvs.add(currEnv)
        }
    }

    builtEnvs.sortWith(String.CASE_INSENSITIVE_ORDER)
    if (mineLocalEnv.isNotBlank()) {
        builtEnvs.add(0, mineLocalEnv)
    }

    return builtEnvs
}
fun isEnvironmentLocal(environment: String): Boolean {
    return environment.endsWith(SUFFIX_OF_LOCAL, true)
}

fun isLocalEnvironmentMine(environment: String, localHostname: String): Boolean {
    return environment.startsWith(localHostname, true)
}