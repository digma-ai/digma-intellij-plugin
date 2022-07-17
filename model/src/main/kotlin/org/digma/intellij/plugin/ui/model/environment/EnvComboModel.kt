package org.digma.intellij.plugin.ui.model.environment

import java.util.*
import javax.swing.AbstractListModel
import javax.swing.ComboBoxModel

class EnvComboModel(private val environmentsSupplier: EnvironmentsSupplier) : AbstractListModel<String>(), ComboBoxModel<String>, EnvironmentsListChangedListener {

    private var environments: List<String> = ArrayList()
    private var selectedItem : String? = null

    init {
        environments = ArrayList(getEnvironments())
        selectedItem = environmentsSupplier.getCurrent()
        environmentsSupplier.addEnvironmentsListChangeListener(this)
        fireContentsChanged(this,0, environments.size)
    }


    private fun getEnvironments():List<String>{
        return environmentsSupplier.getEnvironments()
    }

    override fun getSize(): Int {
        return environments.size
    }

    override fun getElementAt(index: Int): String {
        return environments[index]
    }

    override fun environmentsListChanged(newEnvironments: List<String>) {
        replaceEnvironments(newEnvironments)
    }



    private fun replaceEnvironments(newEnvironments: List<String>) {

        if (environmentsListEquals(newEnvironments,this.environments)){
            return
        }

        this.environments = ArrayList(newEnvironments)

        var newSelectedItem: String? = this.selectedItem
        if (newSelectedItem == null) {
            newSelectedItem = this.environments.firstOrNull()
        } else if (!this.environments.contains(newSelectedItem)) {
            newSelectedItem = this.environments.firstOrNull()
        }


        if (!Objects.equals(newSelectedItem,this.selectedItem)){
            this.selectedItem = newSelectedItem
            environmentsSupplier.setCurrent(selectedItem)
        }

        fireContentsChanged(this, 0, this.environments.size)
    }




    override fun setSelectedItem(anObject: Any?) {
        if (Objects.equals(anObject,this.selectedItem)){
            return
        }

        this.selectedItem = anObject as String?
        environmentsSupplier.setCurrent(selectedItem)
        val i = this.environments.indexOf(this.selectedItem)
        fireContentsChanged(this,i,i+1)
    }

    override fun getSelectedItem(): Any? {
        return selectedItem
    }

    fun refreshEnvironments() {
        //the environment will refresh in the background and fire an event
        //the combo will catch the event and update the list
        environmentsSupplier.refresh()
    }


    private fun environmentsListEquals(envs1: List<String>?, envs2: List<String>?): Boolean {
        if (envs1 == null && envs2 == null) {
            return true
        }
        return if (envs1 != null && envs2 != null && envs1.size == envs2.size) {
            HashSet(envs1).containsAll(envs2)
        } else false
    }
}