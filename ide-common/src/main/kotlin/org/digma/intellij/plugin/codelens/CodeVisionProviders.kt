package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering

const val LIVE_PROVIDER_ID = "DigmaLive"

class LiveCodeLensProvider : DigmaCodeVisionProviderBase() {

    override val id: String
        get() = LIVE_PROVIDER_ID

    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst)

}


class CodeVisionProvider1 : DigmaCodeVisionProviderBase()
class CodeVisionProvider2 : DigmaCodeVisionProviderBase()
class CodeVisionProvider3 : DigmaCodeVisionProviderBase()
class CodeVisionProvider4 : DigmaCodeVisionProviderBase()
class CodeVisionProvider5 : DigmaCodeVisionProviderBase()
class CodeVisionProvider6 : DigmaCodeVisionProviderBase()
class CodeVisionProvider7 : DigmaCodeVisionProviderBase()
class CodeVisionProvider8 : DigmaCodeVisionProviderBase()
class CodeVisionProvider9 : DigmaCodeVisionProviderBase()
class CodeVisionProvider10 : DigmaCodeVisionProviderBase()
class CodeVisionProvider11 : DigmaCodeVisionProviderBase()
class CodeVisionProvider12 : DigmaCodeVisionProviderBase()
class CodeVisionProvider13 : DigmaCodeVisionProviderBase()
class CodeVisionProvider14 : DigmaCodeVisionProviderBase()
class CodeVisionProvider15 : DigmaCodeVisionProviderBase()
