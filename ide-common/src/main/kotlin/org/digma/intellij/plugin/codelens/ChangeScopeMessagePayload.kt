package org.digma.intellij.plugin.codelens

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.lens.CodeLens
import java.beans.ConstructorProperties

data class ChangeScopeMessagePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeLens")
constructor(val codeLens: CodeLens)