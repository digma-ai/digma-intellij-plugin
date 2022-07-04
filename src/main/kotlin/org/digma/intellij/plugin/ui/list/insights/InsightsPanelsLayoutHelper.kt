package org.digma.intellij.plugin.ui.list.insights

import com.jetbrains.rd.util.getOrCreate
import org.jetbrains.annotations.Nullable

/**
 * A helper for layout panels to layout themselves, should only be used for insights list panels.
 * this is a singleton object, which may sound wierd for a layout, but because we know there is only one insights list
 * showing at a time we can use a singleton object, it makes everything less verbose. it should not be used for other
 * lists like the errors list. they should have another helper.
 * if there are going to be more than one insights list showing at the same time then this singleton needs to be converted
 * to a per list instance and be sent as parameter to all kotlin methods that need it.
 * because its a singleton it needs to reset between rebuilds of the list or the panels, the InsightsListCellRenderer
 * is responsible for resting this layout helper every time the list data changes, which means the panels are going to
 * be rebuilt.
 *
 * because the panels are not managed by the same layout manager it is sometimes difficult to align components evenly
 * across all panels.
 * for example the right side icon panel in most insights panels need to be aligned the same, but each of them has
 * different size because of different text. but the panels can share information between them about the size of the text
 * and make sure they all prefer the same size. this is implemented in function insightsIconPanelBorder.
 *
 *
 */
internal object InsightsPanelsLayoutHelper {

    private final val globalAttributes = HashMap<String,Any>()

    private val perObjectAttributes = HashMap<String,HashMap<String,Any>>()

    fun addGlobalAttribute(name: String,value: Any){
        globalAttributes[name] = value
    }

    @Nullable
    fun getGlobalAttribute(name: String): Any? {
        return globalAttributes[name]
    }

    fun addObjectAttribute(objectName:String,name: String,value: Any){
        perObjectAttributes.getOrCreate(objectName, creator = {HashMap<String,Any>()})[name] = value
    }

    @Nullable
    fun getObjectAttribute(objectName:String,name: String):Any?{
        return perObjectAttributes.getOrCreate(objectName,creator = {HashMap<String,Any>()})[name]
    }

    fun reset() {
        globalAttributes.clear()
        perObjectAttributes.clear()
    }


}