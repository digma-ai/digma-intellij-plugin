package org.digma.intellij.plugin.test.system.framework

import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects


val methodCodeObject1 = MethodWithCodeObjects(
    codeObjectId = "org.digma.intellij.plugin.editor.EditorEventsHandler\$_\$isFileNotChangingContext",
    relatedSpansCodeObjectIds = listOf("relatedSpan1, relatedSpan2, relatedSpan3"),
    relatedEndpointCodeObjectIds = listOf("relatedEndpoint1, relatedEndpoint2, relatedEndpoint3")
)


val methodCodeObject2 = MethodWithCodeObjects(
    codeObjectId = "org.digma.intellij.plugin.editor.EditorEventsHandler\$_\$selectionChanged, ",
    relatedSpansCodeObjectIds = listOf("relatedSpan1, relatedSpan2, relatedSpan3"),
    relatedEndpointCodeObjectIds = listOf("relatedEndpoint1, relatedEndpoint2, relatedEndpoint3")
)

val methodCodeObject3 = MethodWithCodeObjects(
    codeObjectId = "org.digma.intellij.plugin.editor.EditorEventsHandler\$_\$isRelevantFile",
    relatedSpansCodeObjectIds = listOf("relatedSpan1, relatedSpan2, relatedSpan3"),
    relatedEndpointCodeObjectIds = listOf("relatedEndpoint1, relatedEndpoint2, relatedEndpoint3")
)



