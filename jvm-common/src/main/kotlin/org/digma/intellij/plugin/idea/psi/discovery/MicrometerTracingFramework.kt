package org.digma.intellij.plugin.idea.psi.discovery

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiTreeUtil
import org.digma.intellij.plugin.common.StringUtils
import org.digma.intellij.plugin.idea.psi.createPsiMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.psi.PsiUtils

class MicrometerTracingFramework {

    companion object {
        val logger = Logger.getInstance(MicrometerTracingFramework::class.java)

        // NEW_SPAN_FQN , @Target(ElementType.METHOD)
        const val NEW_SPAN_FQN = "io.micrometer.tracing.annotation.NewSpan"
        const val MICRO_METER_INST_LIB = "io.micrometer" // TODO: make sure its the right name
        val NEW_SPAN_ATTRIBUTES = listOf("value", "name")

        // OBSERVED_FQN , @Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD })
        const val OBSERVED_FQN = "io.micrometer.observation.annotation.Observed"
        const val OBSERVED_DEPENDENCY_DESCRIPTION = "micrometer.observation"
        const val OBSERVED_INST_LIB = "org.springframework.boot"
        private const val OBSERVED_MAX_LEN_OF_SPAN_NAME = 50

        /**
         * used for renaming span names based on class name and/or method name.
         * the algo: when char is upper case, make it lower and prepend a dash
         * few examples:
         * for input of "HelloController" (class name) it will return "hello-controller"
         * for input of "doSomeWork" (method name) it will return "do-some-work"
         */
        private fun adjustSpanNamePart(spanNamePart: String): String {
            val sb = StringBuilder()
            for ((index, currChar) in spanNamePart.iterator().withIndex()) {
                if (index == 0) {
                    sb.append(Character.toLowerCase(currChar))
                    continue
                }
                if (Character.isUpperCase(currChar)) {
                    sb.append('-')
                    sb.append(Character.toLowerCase(currChar))
                } else {
                    sb.append(currChar)
                }
            }
            return sb.toString()
        }

        private fun convertSpanNameOfObserved(psiMethod: PsiMethod): String {
            // a method in java must have a containing class. (psiMethod.getContainingClass may return null because
            // it supports other languages like groovy and kotlin)
            //TODO: not sure how to behave when no class (for example in Kotlin)

            val sb = StringBuilder()
            psiMethod.containingClass?.let { clazz ->
                if (!clazz.name.isNullOrBlank()) {
                    sb.append(adjustSpanNamePart(clazz.name!!))
                    sb.append('#')
                }
            }

            sb.append(adjustSpanNamePart(psiMethod.name))

            return sb.toString()
        }

        private fun observedMaxSpanName(spanName: String): String {
            return StringUtils.firstPart(spanName, OBSERVED_MAX_LEN_OF_SPAN_NAME)
        }
    }


    fun discoverSpans(project: Project, psiFile: PsiFile): Collection<SpanInfo> {
        val spanInfos = mutableListOf<SpanInfo>()

        val newSpanAnnotationSpans = newSpanAnnotationSpanDiscovery(project, psiFile)
        spanInfos.addAll(newSpanAnnotationSpans)

        val observedAnnotationSpans = observedAnnotationSpanDiscovery(project, psiFile)
        spanInfos.addAll(observedAnnotationSpans)

        return spanInfos
    }


    private fun newSpanAnnotationSpanDiscovery(project: Project, psiFile: PsiFile): Collection<SpanInfo> {

        val spanInfos = mutableListOf<SpanInfo>()

        val newSpanClass = JavaPsiFacade.getInstance(project).findClass(NEW_SPAN_FQN, GlobalSearchScope.allScope(project))
        //maybe the annotation is not in the classpath
        if (newSpanClass != null) {
            val psiMethods = AnnotatedElementsSearch.searchPsiMethods(newSpanClass, GlobalSearchScope.fileScope(psiFile))
//            psiMethods = JavaSpanDiscoveryUtils.filterNonRelevantMethodsForSpanDiscovery(psiMethods)
            for (it in psiMethods) {
                val spanInfo = getSpanInfoFromNewSpanAnnotatedMethod(it)
                spanInfo?.let {
                    spanInfos.add(it)
                }
            }
        }

        return spanInfos
    }


    private fun observedAnnotationSpanDiscovery(project: Project, psiFile: PsiFile): Collection<SpanInfo> {

        val spanInfos = mutableListOf<SpanInfo>()

        val observedAnnotationClass = JavaPsiFacade.getInstance(project).findClass(OBSERVED_FQN, GlobalSearchScope.allScope(project))
        //maybe the annotation is not in the classpath
        if (observedAnnotationClass != null) {
            // TODO: search for classes/interfaces that are annotated with OBSERVED_FQN and even child classes

            val psiMethods = AnnotatedElementsSearch.searchPsiMethods(observedAnnotationClass, GlobalSearchScope.fileScope(psiFile))
//            psiMethods = JavaSpanDiscoveryUtils.filterNonRelevantMethodsForSpanDiscovery(psiMethods)
            for (it in psiMethods) {
                val spanInfo = getSpanInfoFromObservedAnnotatedMethod(it)
                spanInfo?.let {
                    spanInfos.add(it)
                }
            }
        }

        return spanInfos
    }



    fun getSpanInfoFromNewSpanAnnotatedMethod(psiMethod: PsiMethod): SpanInfo? {
        val newSpanAnnotation = psiMethod.getAnnotation(NEW_SPAN_FQN)
        val containingFile = PsiTreeUtil.getParentOfType(psiMethod, PsiFile::class.java)
        val containingClass = psiMethod.containingClass

        //withSpanAnnotation,containingFile and containingClass must not be null because we found this annotation in a search.
        // a method in java must have a containing class. (psiMethod.getContainingClass may return null because
        // it supports other languages like groovy and kotlin)
        if (newSpanAnnotation != null && containingFile != null && containingClass != null) {
            val methodId = createPsiMethodCodeObjectId(psiMethod)
            val containingFileUri = PsiUtils.psiFileToUri(containingFile)

            val tmpSpanName = JavaLanguageUtils.getValueOfFirstMatchingAnnotationAttribute(newSpanAnnotation, NEW_SPAN_ATTRIBUTES, null)
            val spanName = tmpSpanName ?: (containingClass.name + "." + psiMethod.name)
            return SpanInfo(
                JavaLanguageUtils.createSpanIdFromInstLibraryAndSpanName(MICRO_METER_INST_LIB, spanName),
                spanName, methodId, containingFileUri
            )
        }

        //if here then we couldn't completely discover the span
        return null
    }

    fun getSpanInfoFromObservedAnnotatedMethod(psiMethod: PsiMethod): SpanInfo? {
        val observedAnnotationOnMethod = psiMethod.getAnnotation(OBSERVED_FQN)
        val containingFile = PsiTreeUtil.getParentOfType(psiMethod, PsiFile::class.java)
        //TODO: see if class itself is annotated with OBSERVED_FQN - then all its methods as well

        // observedAnnotationOnMethod,containingFile must not be null because we found this annotation in a search.
        if (observedAnnotationOnMethod != null && containingFile != null) {
            val methodId = createPsiMethodCodeObjectId(psiMethod)
            val containingFileUri = PsiUtils.psiFileToUri(containingFile)

            val tmpSpanName = JavaLanguageUtils.getPsiAnnotationAttributeValue(observedAnnotationOnMethod, "contextualName")
            val spanName = observedMaxSpanName(
                if (!tmpSpanName.isNullOrBlank()) {
                    tmpSpanName
                } else {
                    convertSpanNameOfObserved(psiMethod)
                }
            )

            return SpanInfo(
                JavaLanguageUtils.createSpanIdFromInstLibraryAndSpanName(OBSERVED_INST_LIB, spanName),
                spanName, methodId, containingFileUri
            )
        }

        //if here then we couldn't completely discover the span
        return null
    }


}