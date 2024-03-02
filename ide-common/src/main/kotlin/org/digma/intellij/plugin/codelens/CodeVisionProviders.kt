package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering

const val LIVE_PROVIDER_ID = "DigmaLive"

class LiveCodeLensProvider : DigmaCodeVisionProviderBase() {

    override val id: String
        get() = LIVE_PROVIDER_ID

    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst)

    override var myLensId: String? = "LIVE"

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
class CodeVisionProvider16 : DigmaCodeVisionProviderBase()
class CodeVisionProvider17 : DigmaCodeVisionProviderBase()
class CodeVisionProvider18 : DigmaCodeVisionProviderBase()
class CodeVisionProvider19 : DigmaCodeVisionProviderBase()
class CodeVisionProvider20 : DigmaCodeVisionProviderBase()
class CodeVisionProvider21 : DigmaCodeVisionProviderBase()
class CodeVisionProvider22 : DigmaCodeVisionProviderBase()
class CodeVisionProvider23 : DigmaCodeVisionProviderBase()
class CodeVisionProvider24 : DigmaCodeVisionProviderBase()
class CodeVisionProvider25 : DigmaCodeVisionProviderBase()
class CodeVisionProvider26 : DigmaCodeVisionProviderBase()
class CodeVisionProvider27 : DigmaCodeVisionProviderBase()
class CodeVisionProvider28 : DigmaCodeVisionProviderBase()
class CodeVisionProvider29 : DigmaCodeVisionProviderBase()
class CodeVisionProvider30 : DigmaCodeVisionProviderBase()
