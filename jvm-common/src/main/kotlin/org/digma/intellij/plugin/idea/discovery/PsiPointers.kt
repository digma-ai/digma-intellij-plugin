package org.digma.intellij.plugin.idea.discovery

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import org.digma.intellij.plugin.common.ReadActions
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

@Service(Service.Level.PROJECT)
class PsiPointers(val project: Project) {

    private val classPointers = ConcurrentHashMap(mutableMapOf<String, SmartPsiElementPointer<PsiClass>>())

    private var startSpanPsiMethodPointer: SmartPsiElementPointer<PsiMethod>? = null


    fun getPsiClass(project: Project, className: String): PsiClass? {
        val classPointer = getPsiClassPointer(project, className)
        return ReadActions.ensureReadAction(Supplier {classPointer?.element })
    }

    fun getPsiClassPointer(project: Project, className: String): SmartPsiElementPointer<PsiClass>? {
        if (classPointers[className] == null) {
            val classPointer = ReadActions.ensureReadAction(Supplier {
                val psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
                psiClass?.let {
                    SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it)
                }
            })
            classPointer?.let {
                classPointers[className] = it
            }
        }
        return classPointers[className]
    }


    //it's not easy to keep keys for methods coz can't use the PsiClass as key in a map.
    // we don't have many cases that we need to keep a pointer to PsiMethod so specific method for each use case is ok.
    fun getOtelStartSpanMethodPointer(
        project: Project,
        builderClassPointer: SmartPsiElementPointer<PsiClass>
    ): SmartPsiElementPointer<PsiMethod>? {
        val builderClass = ReadActions.ensureReadAction(Supplier { builderClassPointer.element })
        return builderClass?.let {
            getOtelStartSpanMethodPointer(project, it)
        }
    }


    private fun getOtelStartSpanMethodPointer(project: Project, builderClass: PsiClass): SmartPsiElementPointer<PsiMethod>? {
        if (startSpanPsiMethodPointer == null) {
            startSpanPsiMethodPointer = ReadActions.ensureReadAction(Supplier {
                val startSpanMethod: PsiMethod? = findMethodInClass(builderClass, "startSpan") { psiMethod: PsiMethod ->
                    @Suppress("UnstableApiUsage")
                    psiMethod.parameters.isEmpty()
                }
                startSpanMethod?.let {
                    SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it)
                }
            })
        }

        return startSpanPsiMethodPointer
    }
}