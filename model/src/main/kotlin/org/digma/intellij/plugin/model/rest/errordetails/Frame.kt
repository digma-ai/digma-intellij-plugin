package org.digma.intellij.plugin.model.rest.errordetails

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class Frame
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "moduleName",
    "functionName",
    "functionParameterTypes",
    "lineNumber",
    "executedCode",
    "codeObjectId",
    "parameters",
    "repeat",
    "spanName",
    "spanKind",
    "moduleLogicalPath",
    "modulePhysicalPath",
    "className"
)constructor(
    val moduleName: String,
    val functionName: String,
    val functionParameterTypes: List<String>,
    val lineNumber: Int,
    val executedCode: String,
    val codeObjectId: String,
    val parameters: List<ParamStats>,
    val repeat: Int,
    val spanName: String,
    val spanKind: String,
    val moduleLogicalPath: String,
    val modulePhysicalPath: String,
    val className: String?
)
