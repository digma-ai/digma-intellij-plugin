package org.digma.intellij.plugin.idea.index

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.EventDispatcher
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.digma.intellij.plugin.log.Log

/**
 * Finds candidate files for span and endpoint discovery.
 * We could use PsiSearchHelper or CacheManager to search for words, but when? If we search on startup, maybe
 * the index is not ready yet. We can try again, but how many times? there is no indication if we jus can't
 * find anything or indexes are not ready yet or some other issue.
 * But with our own index we can query it. it will index only relevant files, so if it exists, we know it either
 * contains something or not.
 */
class CandidateFilesForDiscoveryDetectionIndex : ScalarIndexExtension<String>() {

    val logger = thisLogger()
    private val myDispatcher = EventDispatcher.create(CandidateFilesDetectionIndexListener::class.java)


    fun addListener(listener: CandidateFilesDetectionIndexListener, parentDisposable: Disposable) {
        myDispatcher.addListener(listener, parentDisposable)
    }

    override fun getName(): ID<String, Void> {
        return CANDIDATE_FILES_INDEX_ID
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file ->
            if (!file.isWritable || !file.isInLocalFileSystem) {
                return@InputFilter false
            }
            //we don't have the kotlin plugin classes in the classpath of this module
            // because it is isolated in case the kotlin plugin is disabled
            file.fileType is JavaFileType ||
                    file.fileType.name.equals("kotlin", ignoreCase = true)
        }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, Void?, FileContent> {
        return MyDataIndexer()
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE


    override fun getVersion(): Int = 1


    inner class MyDataIndexer : DataIndexer<String, Void?, FileContent> {

        private val spanDiscoveryMarkerPairs = listOf(
            //can only check the present of package names, maybe only check io.opentelemetry,
            // but that may find too many false positive candidates.

            // @WithSpan annotation
            "io.opentelemetry.instrumentation.annotations" to "WithSpan",
            // SpanBuilder method call
            "io.opentelemetry.api.trace" to "startSpan",
            // Micrometer @Observed annotation
            "io.micrometer.observation.annotation" to "Observed"
        )

        private val endpointDiscoveryMarkerPairs = listOf(
            // Spring
            "org.springframework.web.bind.annotation" to "GetMapping",
            "org.springframework.web.bind.annotation" to "PostMapping",
            "org.springframework.web.bind.annotation" to "PutMapping",
            "org.springframework.web.bind.annotation" to "DeleteMapping",
            "org.springframework.web.bind.annotation" to "PatchMapping",
            "org.springframework.web.bind.annotation" to "RequestMapping",

            // Micronaut
            "io.micronaut.http.annotation" to "Get",
            "io.micronaut.http.annotation" to "Post",
            "io.micronaut.http.annotation" to "Put",
            "io.micronaut.http.annotation" to "Delete",
            "io.micronaut.http.annotation" to "Patch",
            "io.micronaut.http.annotation" to "Options",
            "io.micronaut.http.annotation" to "Head",
            "io.micronaut.http.annotation" to "Trace",

            // gRPC (interface name, not annotation — but still works)
            "io.grpc" to "BindableService",

            // JAX-RS Jakarta
            "jakarta.ws.rs" to "ApplicationPath",
            "jakarta.ws.rs" to "Path",
            "jakarta.ws.rs" to "GET",
            "jakarta.ws.rs" to "POST",
            "jakarta.ws.rs" to "PUT",
            "jakarta.ws.rs" to "DELETE",
            "jakarta.ws.rs" to "HEAD",
            "jakarta.ws.rs" to "OPTIONS",
            "jakarta.ws.rs" to "PATCH",

            // JAX-RS javax
            "javax.ws.rs" to "ApplicationPath",
            "javax.ws.rs" to "Path",
            "javax.ws.rs" to "GET",
            "javax.ws.rs" to "POST",
            "javax.ws.rs" to "PUT",
            "javax.ws.rs" to "DELETE",
            "javax.ws.rs" to "HEAD",
            "javax.ws.rs" to "OPTIONS",
            "javax.ws.rs" to "PATCH",

            // Ktor routing builder
            "io.ktor.server.routing" to "get",
            "io.ktor.server.routing" to "post",
            "io.ktor.server.routing" to "put",
            "io.ktor.server.routing" to "delete",
            "io.ktor.server.routing" to "patch",
            "io.ktor.server.routing" to "options",
            "io.ktor.server.routing" to "head"
        )


        override fun map(inputData: FileContent): MutableMap<String, Void?> {

            val result = mutableMapOf<String, Void?>()
            val fileText = inputData.contentAsText

            if (isCandidateForSpanDiscovery(fileText)) {
                Log.trace(logger, inputData.project, "Found candidate for span discovery: {}", inputData.file.path)
                result[CANDIDATE_FILES_INDEX_KEY_SPAN] = null
            }

            if (isCandidateForEndpointDiscovery(fileText)) {
                Log.trace(logger, inputData.project, "Found candidate for endpoint discovery: {}", inputData.file.path)
                result[CANDIDATE_FILES_INDEX_KEY_ENDPOINT] = null
            }

            if (result.isNotEmpty()) {
                myDispatcher.multicaster.fileUpdated(inputData.file, result.keys.toList())
            }

            return result

        }

        private fun isCandidateForSpanDiscovery(text: CharSequence): Boolean {
            return spanDiscoveryMarkerPairs.any { (pkg, symbol) ->
                text.contains(pkg) && text.contains(symbol)
            }
        }

        private fun isCandidateForEndpointDiscovery(text: CharSequence): Boolean {
            return endpointDiscoveryMarkerPairs.any { (pkg, symbol) ->
                text.contains(pkg) && text.contains(symbol)
            }
        }
    }
}