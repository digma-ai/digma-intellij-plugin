package org.digma.intellij.plugin.common

import org.apache.commons.lang3.time.StopWatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer


fun stopWatchStart():StopWatch{
    return StopWatch.createStarted()
}

fun stopWatchStop(stopWatch: StopWatch,action:Consumer<Long>){
    stopWatch.stop()
    action.accept(stopWatch.getTime(TimeUnit.MILLISECONDS))
}
