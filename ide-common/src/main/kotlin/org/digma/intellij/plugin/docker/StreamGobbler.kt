package org.digma.intellij.plugin.docker

import org.jsoup.helper.Consumer
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class StreamGobbler(private val inputStream: InputStream, private val consumer: Consumer<String>) : Runnable {
    override fun run() {

        BufferedReader(InputStreamReader(inputStream)).lines()
            .forEach {
                consumer.accept(it)
            }

    }
}