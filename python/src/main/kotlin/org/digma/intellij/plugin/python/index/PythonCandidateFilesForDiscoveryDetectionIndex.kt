package org.digma.intellij.plugin.python.index

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.util.EventDispatcher
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.python.PythonFileType
import org.digma.intellij.plugin.discovery.index.CANDIDATE_FILES_INDEX_KEY_ENDPOINT
import org.digma.intellij.plugin.discovery.index.CANDIDATE_FILES_INDEX_KEY_SPAN
import org.digma.intellij.plugin.discovery.index.CandidateFilesDetectionIndexListener
import org.digma.intellij.plugin.log.Log

/**
 * Finds candidate files for span and endpoint discovery.
 * We could use PsiSearchHelper or CacheManager to search for words, but when? If we search on startup, maybe
 * the index is not ready yet. We can try again, but how many times? There is no indication if we just can't
 * find anything or indexes are not ready yet or some other issue.
 * But with our own index we can query it. It will index only relevant files, so if it exists, we know it either
 * contains something or not.
 */
class PythonCandidateFilesForDiscoveryDetectionIndex : ScalarIndexExtension<String>() {

    val logger = thisLogger()
    private val myDispatcher = EventDispatcher.create(CandidateFilesDetectionIndexListener::class.java)


    fun addListener(listener: CandidateFilesDetectionIndexListener, parentDisposable: Disposable) {
        myDispatcher.addListener(listener, parentDisposable)
    }

    override fun getName(): ID<String, Void> {
        return PYTHON_CANDIDATE_FILES_INDEX_ID
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file ->
            if (!file.isWritable || !file.isInLocalFileSystem) {
                return@InputFilter false
            }
            //we don't have the kotlin plugin classes in the classpath of this module
            // because it is isolated in case the kotlin plugin is disabled
            file.fileType is PythonFileType
        }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, Void?, FileContent> {
        return MyDataIndexer()
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE


    override fun getVersion(): Int = 2


    inner class MyDataIndexer : DataIndexer<String, Void?, FileContent> {

        private val spanDiscoveryMarkers = listOf(
            "start_as_current_span",
            "start_span"
        )

        private val endpointDiscoveryMarkers = listOf<String>()


        override fun map(inputData: FileContent): MutableMap<String, Void?> {

            val result = mutableMapOf<String, Void?>()
            val project = inputData.project
            val file = inputData.file
            // Skip library classes
            if (ProjectFileIndex.getInstance(project).isInLibraryClasses(file)) {
                return result
            }


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
            return spanDiscoveryMarkers.any { symbol -> text.contains(symbol, true) }
        }

        private fun isCandidateForEndpointDiscovery(text: CharSequence): Boolean {
            return endpointDiscoveryMarkers.any { symbol -> text.contains(symbol, true) }
        }
    }
}