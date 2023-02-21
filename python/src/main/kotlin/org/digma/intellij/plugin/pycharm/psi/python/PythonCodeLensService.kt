package org.digma.intellij.plugin.pycharm.psi.python

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

class PythonCodeLensService(private val project: Project): Disposable {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): PythonCodeLensService {
            return project.getService(PythonCodeLensService::class.java)
        }
    }

    override fun dispose() {

    }


    fun environmentChanged(newEnv: String) {
//        codeLensCache.clear()
//        ApplicationManager.getApplication().runReadAction {
//            val fileEditor = FileEditorManager.getInstance(project).selectedEditor
//            if (fileEditor != null) {
//                val file = fileEditor.file
//                val psiFile = PsiManager.getInstance(project).findFile(file)
//                if (psiFile != null) {
//                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
//                }
//            }
//        }
    }

}