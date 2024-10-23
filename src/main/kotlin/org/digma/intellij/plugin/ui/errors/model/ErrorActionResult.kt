package org.digma.intellij.plugin.ui.errors.model


class ActionError (val message: String?);
class ErrorActionResult(val id: String, isSuccess: Boolean, val error: ActionError?)
{
 val status: String = if(isSuccess) "success" else "failure"
};
