package org.digma.intellij.plugin.ui.model.environment

import javax.swing.AbstractListModel
import javax.swing.ComboBoxModel

object EnvComboModel : AbstractListModel<String>(), ComboBoxModel<String>, EnvironmentsListChangedListener {

    private var environmentsSupplier: EnvironmentsSupplier? = null

    private var environments: List<String> = ArrayList()
    private var selectedItem : String? = null


    fun initialize(environmentsSupplier: EnvironmentsSupplier) {
        this.environmentsSupplier = environmentsSupplier
        environments = ArrayList(getEnvironments())
        selectedItem = environmentsSupplier.getCurrent()
        environmentsSupplier.addEnvironmentsListChangeListener(this)
        fireContentsChanged(this,0, environments.size)
    }

    private fun getEnvironments():List<String>{
        return environmentsSupplier?.getEnvironments() ?: ArrayList()
    }

    override fun getSize(): Int {
        return environments.size
    }

    override fun getElementAt(index: Int): String {
        return environments[index]
    }

    override fun environmentsListChanged(newEnvironments: List<String>) {
        this.environments = ArrayList(newEnvironments)
        fireContentsChanged(this,0, newEnvironments.size)
        var newSelectedItem: String? = this.selectedItem
        if (newSelectedItem == null){
            newSelectedItem = this.environments.firstOrNull()
        }else if(!this.environments.contains(newSelectedItem)){
            newSelectedItem = this.environments.firstOrNull()
        }

        setSelectedItem(newSelectedItem)
    }

    override fun setSelectedItem(anObject: Any?) {
        this.selectedItem = anObject as String?
        environmentsSupplier?.setCurrent(selectedItem)
    }

    override fun getSelectedItem(): Any? {
        return selectedItem
    }

    fun refreshEnvironments() {
        //refresh is called when the combobox popup is opened.
        //if environmentsSupplier will actually refresh it should fire an environmentsListChanged event that should then
        //update this model
        environmentsSupplier?.refresh()
    }
}