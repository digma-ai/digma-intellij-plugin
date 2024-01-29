package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilder

class TestsIndexTemplateBuilder :
    BaseIndexTemplateBuilder(TEST_APP_RESOURCE_FOLDER_NAME, TESTS_APP_INDEX_TEMPLATE_NAME) {

    override fun addAppSpecificEnvVariable(project: Project, data: HashMap<String, Any>) {
        //add specific data for the tests app
    }

}