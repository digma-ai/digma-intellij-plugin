package org.digma.intellij.plugin.idea.psi.discovery.span

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.digma.intellij.plugin.idea.psi.GLOBAL_OPENTELEMETY_FQN
import org.digma.intellij.plugin.idea.psi.OPENTELEMETY_FQN
import org.digma.intellij.plugin.idea.psi.TRACER_FQN
import org.digma.intellij.plugin.idea.psi.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.idea.psi.WITH_SPAN_INST_LIBRARY_1
import org.digma.intellij.plugin.idea.psi.WITH_SPAN_INST_LIBRARY_2
import org.digma.intellij.plugin.idea.psi.WITH_SPAN_INST_LIBRARY_3
import org.digma.intellij.plugin.idea.psi.WITH_SPAN_INST_LIBRARY_4
import org.digma.intellij.plugin.idea.psi.WITH_SPAN_INST_LIBRARY_5
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.createSpanIdFromInstLibraryAndSpanName
import org.digma.intellij.plugin.idea.psi.createSpanNameForWithSpanAnnotation
import org.digma.intellij.plugin.idea.psi.getExpressionValue
import org.digma.intellij.plugin.idea.psi.isMethodWithFirstArgumentString
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.getUCallExpression
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.util.isMethodCall


internal fun getSpanInfoFromWithSpanAnnotatedMethod(uMethod: UMethod): List<SpanInfo> {

    val withSpanAnnotation: UAnnotation? =
        uMethod.uAnnotations.find { uAnnotation: UAnnotation -> uAnnotation.qualifiedName == WITH_SPAN_ANNOTATION_FQN }
    val containingClass: UClass? = uMethod.getContainingUClass()
    val containingFile = uMethod.getContainingUFile()

    if (withSpanAnnotation != null && containingFile != null && containingClass != null) {

        val methodId: String = createMethodCodeObjectId(uMethod)
        val containingFileUri = PsiUtils.psiFileToUri(containingFile.sourcePsi)

        val spanName: String = createSpanNameForWithSpanAnnotation(uMethod, withSpanAnnotation, containingClass)

        val spanInfos = mutableListOf<SpanInfo>()
        spanInfos.add(SpanInfo(createSpanIdFromInstLibraryAndSpanName(WITH_SPAN_INST_LIBRARY_1, spanName), spanName, methodId, containingFileUri));
        spanInfos.add(SpanInfo(createSpanIdFromInstLibraryAndSpanName(WITH_SPAN_INST_LIBRARY_2, spanName), spanName, methodId, containingFileUri));
        spanInfos.add(SpanInfo(createSpanIdFromInstLibraryAndSpanName(WITH_SPAN_INST_LIBRARY_3, spanName), spanName, methodId, containingFileUri));
        spanInfos.add(SpanInfo(createSpanIdFromInstLibraryAndSpanName(WITH_SPAN_INST_LIBRARY_4, spanName), spanName, methodId, containingFileUri));
        spanInfos.add(SpanInfo(createSpanIdFromInstLibraryAndSpanName(WITH_SPAN_INST_LIBRARY_5, spanName), spanName, methodId, containingFileUri));
        return spanInfos
    } else {
        return emptyList()
    }
}


//the reference to startSpan method as in otelTracer?.spanBuilder(spanName)?.startSpan()
fun getSpanInfoFromStartSpanMethodReference(project: Project, uReferenceExpression: UReferenceExpression): SpanInfo? {

    val containingMethod = uReferenceExpression.getContainingUMethod()
    val containingFile = uReferenceExpression.getContainingUFile()

    if (containingMethod != null && containingFile != null) {

        val methodId = createMethodCodeObjectId(containingMethod)
        val containingFileUri = PsiUtils.psiFileToUri(containingFile.sourcePsi)

        //receiver is the reference to SpanBuilder , it may be spanBuilder(spanName) method call or variable
        val uExpression = uReferenceExpression.getUCallExpression()?.receiver
        if (uExpression is UReferenceExpression) {
            return getSpanInfoFromSpanBuilderReferenceExpression(project, uExpression, methodId, containingFileUri);
        }
    }

    return null
}


fun getSpanInfoFromSpanBuilderReferenceExpression(
    project: Project,
    spanBuilderReference: UReferenceExpression,
    methodId: String,
    containingFileUri: String,
): SpanInfo? {


    if (isSpanBuilderMethodCall(spanBuilderReference)) {
        //spanBuilderReference.getUCallExpression() should not be null here
        getSpanInfoFromSpanBuilderMethodCallExpression(project, spanBuilderReference.getUCallExpression()!!, methodId, containingFileUri)
    }


    return null

}


fun getSpanInfoFromSpanBuilderMethodCallExpression(
    project: Project,
    spanBuilderMethodCall: UCallExpression,
    methodId: String,
    containingFileUri: String,
): SpanInfo? {

    val spanName = getExpressionValue(spanBuilderMethodCall.getArgumentForParameter(0)!!)
    val instLibrary = spanName?.let {

        val tracerReferenceExpression = spanBuilderMethodCall.receiver
        return@let when (tracerReferenceExpression) {
            is UReferenceExpression -> {
                getInstLibraryFromTracerReferenceExpression(project, tracerReferenceExpression)
            }

            else -> {
                null
            }
        }
    }


    if (spanName != null && instLibrary != null) {
        val spanId: String = createSpanIdFromInstLibraryAndSpanName(instLibrary, spanName)
        return SpanInfo(spanId, spanName, methodId, containingFileUri)
    }


    return null

}


fun getInstLibraryFromTracerReferenceExpression(project: Project, tracerReference: UReferenceExpression): String? {

    val psiElement = tracerReference.resolve()
    if (psiElement != null && psiElement.toUElement() is UVariable) {
        return getInstLibraryFromTracerVariable(project, psiElement.toUElementOfType<UVariable>()!!)
    } else if (psiElement != null && psiElement.toUElement() is UMethod) {
        return getInstLibraryFromGetTracerMethodCall(project, tracerReference.getUCallExpression()!!)
    }

    return null
}


//this.otelTracer = openTelemetry!!.getTracer("OwnerController")
fun getInstLibraryFromTracerVariable(project: Project, tracerVariable: UVariable): String? {

    if (tracerVariable.sourcePsi == null) {
        return null
    }

    val tracerReferences = ReferencesSearch.search(tracerVariable.sourcePsi!!, GlobalSearchScope.projectScope(project)).findAll()
        .map { psiReference: PsiReference -> psiReference.element.toUElement() }

    //try to find the getTracer method in expressions like:
    // otelTracer = openTelemetry!!.getTracer("OwnerController")
    // otelTracer = null
    // try to find the first non-null result
    tracerReferences.forEach { uElement ->

        val getTracerCall = uElement?.getParentOfType<UBinaryExpression>()?.rightOperand.getUCallExpression()
        if (getTracerCall != null && getTracerCall.isMethodCall()) {
            val instLibrary = getInstLibraryFromGetTracerMethodCall(project, getTracerCall)
            if (instLibrary != null) {
                return instLibrary
            }
        }
    }

    return null
}

fun getInstLibraryFromGetTracerMethodCall(project: Project, uCallExpression: UCallExpression): String? {

    val getTracerMethod = uCallExpression.resolve()?.toUElement()
    if (getTracerMethod != null && getTracerMethod is UMethod) {

        if (isMethodWithFirstArgumentString(getTracerMethod, "getTracer", OPENTELEMETY_FQN) ||
            isMethodWithFirstArgumentString(getTracerMethod, "getTracer", GLOBAL_OPENTELEMETY_FQN)
        ) {
            return getExpressionValue(uCallExpression.getArgumentForParameter(0)!!);
        }
    }

    return null
}


fun isSpanBuilderMethodCall(spanBuilderReference: UReferenceExpression): Boolean {
    val isMethodCall: Boolean = spanBuilderReference.getUCallExpression()?.isMethodCall() ?: false
    val uElement = spanBuilderReference.resolveToUElement()
    return isMethodCall && uElement != null && isMethodWithFirstArgumentString(uElement, "spanBuilder", TRACER_FQN)
}