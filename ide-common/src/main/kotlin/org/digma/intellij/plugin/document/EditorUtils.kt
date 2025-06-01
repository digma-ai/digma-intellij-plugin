package org.digma.intellij.plugin.document

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.isValidVirtualFile

//must be called on EDT
fun getSelectedTextEditorForFile(project: Project, virtualFile: VirtualFile, fileEditorManager: FileEditorManager? = null): Editor? {
    EDT.assertIsDispatchThread()
    if(!isValidVirtualFile(virtualFile)){
        return null
    }
    val editorManager = fileEditorManager ?: FileEditorManager.getInstance(project)
    val selectedEditor = editorManager.getSelectedEditor(virtualFile)
    return selectedEditor?.takeIf { it is TextEditor }?.let { (it as TextEditor).editor }
}