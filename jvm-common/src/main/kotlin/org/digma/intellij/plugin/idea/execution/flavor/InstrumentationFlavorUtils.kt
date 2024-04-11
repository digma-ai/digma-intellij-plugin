package org.digma.intellij.plugin.idea.execution.flavor


fun hasSupportedGradleTasks(taskNames: Set<String>, tasksToFind: List<String>): Boolean {
    return containsGradleTasks(taskNames, tasksToFind)
}

fun hasSupportedMavenGoals(goalNames: Set<String>, goalsToFind: List<String>): Boolean {
    val result = containsMavenGoals(goalNames, goalsToFind)
    //special check for "exec:exec", it is supported only if it also has a vm option -Dexec.executable=java
    if (result && goalNames.contains("exec:exec")) {
        return goalNames.contains("-Dexec.executable=java")
    }
    return result
}


//task may be for example: bootRun, :bootRun. when running a java main method from intellij
// context menu intellij generates a task called XX.mainXX that's why we check task.contains(".$it"
private fun containsGradleTasks(taskNames: Set<String>, toFind: List<String>): Boolean {
    return taskNames.any { task ->
        toFind.any {
            task == it || task.endsWith(":$it") || task.contains(".$it")
        }
    }
}


private fun containsMavenGoals(goalNames: Set<String>, toFind: List<String>): Boolean {
    return goalNames.any { goal ->
        toFind.any {
            val parts = it.split('~')
            if (parts.size == 2) {
                goal.contains(parts[0]) && goal.endsWith(parts[1])
            } else {
                goal == it || goal.endsWith(it)
            }
        }
    }
}
