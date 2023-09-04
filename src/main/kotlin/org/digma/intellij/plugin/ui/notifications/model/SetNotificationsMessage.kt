package org.digma.intellij.plugin.ui.notifications.model

import org.digma.intellij.plugin.ui.jcef.model.BasePayloadMessage
import org.digma.intellij.plugin.ui.jcef.model.Payload


class SetNotificationsMessage(type: String, action: String, payload: Payload) : BasePayloadMessage(type, action, payload)
