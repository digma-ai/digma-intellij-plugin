package org.digma.intellij.plugin.ui.list

import com.jetbrains.rd.util.getOrCreate
import org.jetbrains.annotations.Nullable

/**
 * A helper for layout panels to layout themselves.
 * because the panels in PanlesList are not managed by the same layout manager it is sometimes difficult to align components evenly
 * across all panels.
 * for example: the right side icon panel in most insights panels need to be aligned the same, but each of them has
 * different size because of different text. but the panels can share information between them about the size of the text
 * and make sure they all prefer the same size.
 */

@Suppress("unused")
class PanelsLayoutHelper {

    private val globalAttributes = HashMap<String,Any>()

    private val perObjectAttributes = HashMap<String,HashMap<String,Any>>()

    fun addGlobalAttribute(name: String,value: Any){
        globalAttributes[name] = value
    }

    @Nullable
    fun getGlobalAttribute(name: String): Any? {
        return globalAttributes[name]
    }

    fun addObjectAttribute(objectName:String,name: String,value: Any){
        perObjectAttributes.getOrCreate(objectName, creator = {HashMap()})[name] = value
    }

    @Nullable
    fun getObjectAttribute(objectName:String,name: String):Any?{
        return perObjectAttributes.getOrCreate(objectName,creator = {HashMap()})[name]
    }

//    fun reset() {
//        globalAttributes.clear()
//        perObjectAttributes.clear()
//    }


}