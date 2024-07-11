package org.digma.intellij.plugin.process

import kotlin.time.Duration

data class ProcessResult(val success: Boolean, val canceled: Boolean, val duration: Duration, val error: Throwable?)
