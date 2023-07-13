package org.digma.intellij.plugin.ui.model.environment

interface EnvironmentsSupplier {

    fun getEnvironments(): List<String>

    fun setCurrent(selectedItem: String?)

    /**
     * a variant of setCurrent that notifies all listeners not to update the insights view. and will run a task right
     * after the environment was changed so that this task will see the new environment, the task should run even if the
     * environment didn't change, so this task must run if its not null
     */
    fun setCurrent(selectedItem: String, refreshInsightsView: Boolean, taskToRunAfterChange: Runnable?)

    fun getCurrent(): String?

    fun refreshNowOnBackground()
}