package org.digma.intellij.plugin.document

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.common.EDT

//must be called on EDT
fun getSelectedTextEditorForFile(project: Project, virtualFile: VirtualFile, fileEditorManager: FileEditorManager? = null): Editor? {
    EDT.assertIsDispatchThread()
    val editorManager = fileEditorManager ?: FileEditorManager.getInstance(project)
    val selectedEditor = editorManager.getSelectedEditor(virtualFile)
    return selectedEditor?.takeIf { it is TextEditor }?.let { (it as TextEditor).editor }
}