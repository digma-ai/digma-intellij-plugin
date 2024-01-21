package org.digma.intellij.plugin.ui.tests.model

import org.digma.intellij.plugin.ui.jcef.model.BasePayloadMessage
import org.digma.intellij.plugin.ui.jcef.model.Payload


class SetLatestTestsMessage(type: String, action: String, payload: Payload) : BasePayloadMessage(type, action, payload)
