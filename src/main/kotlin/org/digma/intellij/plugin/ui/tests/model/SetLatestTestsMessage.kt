package org.digma.intellij.plugin.ui.tests.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import org.digma.intellij.plugin.ui.jcef.model.BasePayloadMessage
import org.digma.intellij.plugin.ui.jcef.model.Payload

const val SET_LATEST_TESTS_MESSAGE_NAME = "TESTS/SPAN_SET_LATEST_DATA"

class SetLatestTestsMessage(payload: Payload) : BasePayloadMessage(JCEFGlobalConstants.REQUEST_MESSAGE_TYPE, SET_LATEST_TESTS_MESSAGE_NAME, payload)
