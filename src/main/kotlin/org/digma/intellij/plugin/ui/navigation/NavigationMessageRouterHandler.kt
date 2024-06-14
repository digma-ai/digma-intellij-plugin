package org.digma.intellij.plugin.ui.navigation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.editor.EditorRangeHighlighter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.BaseCommonMessageRouterHandler

class NavigationMessageRouterHandler(project: Project): BaseCommonMessageRouterHandler(project) {



    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        Log.log(logger::trace, project, "got action '$action' with message $requestJsonNode")

        when (action) {

            "NAVIGATION/AUTOFIX_MISSING_DEPENDENCY" -> {
                fixMissingDependencies(requestJsonNode)
            }

            "NAVIGATION/ADD_ANNOTATION" -> {
                addAnnotation(requestJsonNode)
            }

            "NAVIGATION/GO_TO_CODE_LOCATION" -> {
                goToCode(requestJsonNode)
            }

            "NAVIGATION/HIGHLIGHT_METHOD_IN_EDITOR" -> {
                highlightMethod(requestJsonNode)
            }

            "NAVIGATION/CLEAR_HIGHLIGHTS_IN_EDITOR" -> {
                clearHighlight()
            }

            else -> {
                return false
            }
        }

        return true
    }

    private fun clearHighlight() {
        EditorRangeHighlighter.getInstance(project).clearAllHighlighters()
    }

    private fun highlightMethod(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let { pl ->
            val methodId = pl.get("methodId").asText()
            EditorRangeHighlighter.getInstance(project).highlightMethod(CodeObjectsUtil.stripMethodPrefix(methodId))
        }
    }

    private fun goToCode(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let { pl ->
            val codeDetails = pl.get("codeDetails")
            codeDetails?.takeIf { codeDetails !is NullNode }.let { cd ->
                val codeDetailsObj = objectMapper.readTree(cd.toString())
                val codeObjectId = codeDetailsObj.get("codeObjectId").asText()
                NavigationService.getInstance(project).navigateToCode(codeObjectId)
            }
        }
    }


    private fun fixMissingDependencies(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let {
            val methodId = payload.get("methodId").asText()
            NavigationService.getInstance(project).fixMissingDependencies(CodeObjectsUtil.stripMethodPrefix(methodId))
        }
    }

    private fun addAnnotation(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let {
            val methodId = payload.get("methodId").asText()
            NavigationService.getInstance(project).addAnnotation(CodeObjectsUtil.stripMethodPrefix(methodId))
        }
    }

}
