package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.codeVision.CodeVisionProvider
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupDefaultSettingModel
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingModel
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider
import com.intellij.lang.Language

const val DIGMA_CODE_LENS_GROUP_ID = "Digma"

class DigmaCodeVisionSettingsProvider : CodeVisionGroupSettingProvider {
    override val groupId: String
        get() = DIGMA_CODE_LENS_GROUP_ID

    override val description: String
        get() = "Digma code visions"
    override val groupName: String
        get() = "Digma"


    override fun createSettingsModel(isEnabled: Boolean, providers: List<CodeVisionProvider<*>>): CodeVisionGroupSettingModel {
        return MyModel(groupName, groupId, description, Language.findLanguageByID("JAVA"), isEnabled, providers)
    }
}

//todo: can't create a real preview code for group settings
class MyModel(
    name: String,
    groupId: String,
    description: String?,
    previewLanguage: Language?,
    isEnabled: Boolean,
    providers: List<CodeVisionProvider<*>>,
) :
    CodeVisionGroupDefaultSettingModel(name, groupId, description, previewLanguage, isEnabled, providers) {

    override val previewText: String?
        get() = "class MyClass{\n" +
                "\n" +
                "    Error Hotspot\n" +
                "    public void myMethod(){\n" +
                "\n" +
                "    }\n" +
                "}"

}