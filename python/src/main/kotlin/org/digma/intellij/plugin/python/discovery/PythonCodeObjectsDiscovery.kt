@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.python.discovery

import com.intellij.lang.Language
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.python.psi.PyAssignmentStatement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.common.GlobalSearchScopeProvider
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.python.PythonAdditionalIdsProvider
import org.digma.intellij.plugin.python.PythonMethodInfoDisplayNameProvider
import org.digma.intellij.plugin.python.createPythonMethodCodeObjectId
import org.digma.intellij.plugin.python.createSpanId
import org.digma.intellij.plugin.python.getPythonMethodNameFromMethodId
import org.digma.intellij.plugin.python.getRelativePath
import org.digma.intellij.plugin.python.isPythonMethodId
import java.io.File
import kotlin.coroutines.coroutineContext

const val PYTHON_MODULE_NAME_VARIABLE = "__name__"
const val OPENTELEMETRY_TRACER_FQN = "opentelemetry.trace.Tracer"
const val OPENTELEMETRY_START_AS_CURRENT_SPAN_FUNC_NAME = "start_as_current_span"
const val OPENTELEMETRY_START_SPAN_FUNC_NAME = "start_span"
const val OPENTELEMETRY_GET_TRACER_FUNC_NAME = "get_tracer"
const val SITE_PACKAGES_DIR_NAME = "site-packages"


const val UNKNOWN_INST_LIBRARY = "UNKNOWN_INST_LIBRARY"
const val UNKNOWN_SPAN_NAME = "UNKNOWN_SPAN_NAME"

suspend fun buildDocumentInfo(project: Project, pyFile: PyFile, fileUrl: String, language: Language): DocumentInfo? {

    val methodInfos = mutableMapOf<String, MethodInfo>()

    val topLevelFunctions = smartReadAction(project) { pyFile.topLevelFunctions.filter { it.name != null } }
    val topLevelClasses = smartReadAction(project) { pyFile.topLevelClasses }

    coroutineContext.ensureActive()
    topLevelClasses.forEach { pyClass ->
        val classFunctions = readAction { pyClass.methods.filter { it.name != null } }
        classFunctions.forEach { function ->
            coroutineContext.ensureActive()
            readAction {
                val methodInfo = functionDiscovery(project, fileUrl, function)
                methodInfos.put(methodInfo.id, methodInfo)
            }
        }
    }

    coroutineContext.ensureActive()
    topLevelFunctions.forEach { pyFunction ->
        coroutineContext.ensureActive()
        readAction {
            val methodInfo = functionDiscovery(project, fileUrl, pyFunction)
            methodInfos.put(methodInfo.id, methodInfo)
        }
    }

    coroutineContext.ensureActive()
    spanDiscovery(project, pyFile, fileUrl, methodInfos)
    return DocumentInfo(fileUrl, methodInfos, language.id)
}


@RequiresReadLock(generateAssertion = false)
fun functionDiscovery(project: Project, fileUrl: String, pyFunction: PyFunction): MethodInfo {
    val methodId = createPythonMethodCodeObjectId(project, pyFunction)
    val name = pyFunction.name ?: ""
    val containingClassName = pyFunction.containingClass?.name ?: ""
    val namespace = pyFunction.qualifiedName?.substringBefore(".py") ?: ""
    val methodInfo = MethodInfo(methodId, name, containingClassName, namespace, fileUrl)
    methodInfo.additionalIdsProvider = PythonAdditionalIdsProvider()
    methodInfo.displayNameProvider = PythonMethodInfoDisplayNameProvider()
    return methodInfo
}


suspend fun spanDiscovery(
    project: Project,
    pyFile: PyFile,
    fileUri: String,
    methods: MutableMap<String, MethodInfo>
) {
    spanDiscovery(project, pyFile, fileUri, OPENTELEMETRY_START_AS_CURRENT_SPAN_FUNC_NAME, methods)
    coroutineContext.ensureActive()
    spanDiscovery(project, pyFile, fileUri, OPENTELEMETRY_START_SPAN_FUNC_NAME, methods)
}


suspend fun spanDiscovery(
    project: Project,
    pyFile: PyFile,
    fileUri: String,
    tracerMethodName: String,
    methodInfoMap: MutableMap<String, MethodInfo>
) {
    val startSpanFunction = smartReadAction(project) {
        findFunctionByClassNameAndFunctionName(project, OPENTELEMETRY_TRACER_FQN, tracerMethodName) { ProjectScope.getAllScope(project) }
    } ?: return


    coroutineContext.ensureActive()

    val references = smartReadAction(project) {
        ReferencesSearch.search(startSpanFunction, GlobalSearchScope.fileScope(pyFile))
    }
    coroutineContext.ensureActive()

    references.forEach { psiReference: PsiReference ->
        val pyCallExpression = readAction {
            PsiTreeUtil.getParentOfType(psiReference.element, PyCallExpression::class.java)
        }
        pyCallExpression?.let { callExpression ->
            coroutineContext.ensureActive()
            val spanInfos = readAction {
                discoverSpanFromStartSpanMethodCallExpression(project, pyFile, callExpression, fileUri)
            }
            if (spanInfos.isNotEmpty()) {
                val methodInfo = methodInfoMap[spanInfos[0].containingMethodId]
                methodInfo?.spans?.addAll(spanInfos)
            }
        }
    }
}

@RequiresReadLock(generateAssertion = false)
fun discoverSpanFromStartSpanMethodCallExpression(
    project: Project,
    pyFile: PyFile,
    callExpression: PyCallExpression,
    fileUri: String
): List<SpanInfo> {
    val result = mutableListOf<SpanInfo>()
    val args = callExpression.arguments
    val function = PsiTreeUtil.getParentOfType(callExpression, PyFunction::class.java)

    val receiver = callExpression.getReceiver(null)
    if (args.isNotEmpty() && function != null && receiver != null) {
        val methodId = createPythonMethodCodeObjectId(project, function)
        val spanName = getSpanNameFromNameArgument(args[0])
        val instLibrary = getInstLibraryFromReceiverExpression(receiver)
        if (PYTHON_MODULE_NAME_VARIABLE == instLibrary) {
            var spanId = createSpanId("__main__", spanName)
            result.add(SpanInfo(spanId, spanName, methodId, fileUri, function.startOffset))

            var relativePath = getRelativePath(project, pyFile)
            if (relativePath.lastIndexOf(".py") >= 0) {
                relativePath = relativePath.substringBeforeLast(".py")
            }
            relativePath = relativePath.replace(File.separator, ".") //should work on linux and windows

            spanId = createSpanId(relativePath, spanName)
            result.add(SpanInfo(spanId, spanName, methodId, fileUri, function.startOffset))

            if (relativePath.contains(".")) {
                relativePath = relativePath.substringAfter(".")
                spanId = createSpanId(relativePath, spanName)
                result.add(SpanInfo(spanId, spanName, methodId, fileUri, function.startOffset))
            }
        } else {
            val spanId = createSpanId(instLibrary, spanName)
            result.add(SpanInfo(spanId, spanName, methodId, fileUri, function.startOffset))
        }
    }

    return result
}

@RequiresReadLock(generateAssertion = false)
fun getSpanNameFromNameArgument(pyExpression: PyExpression): String {
    if (pyExpression is PyStringLiteralExpression) {
        return getStringFromStringLiteralExpression(pyExpression)
    } else if (pyExpression is PyReferenceExpression) {
        return getSpanNameFromReferenceExpression(pyExpression)
    }

    return UNKNOWN_SPAN_NAME
}

@RequiresReadLock(generateAssertion = false)
fun getSpanNameFromReferenceExpression(referenceExpression: PyReferenceExpression): String {
    if (PYTHON_MODULE_NAME_VARIABLE == referenceExpression.text) {
        return PYTHON_MODULE_NAME_VARIABLE
    }

    val pyElement = referenceExpression.reference.resolve()
    if (pyElement is PyTargetExpression) {
        val assignedValue = pyElement.findAssignedValue()
        if (assignedValue is PyStringLiteralExpression) {
            return getStringFromStringLiteralExpression(assignedValue)
        } else if (assignedValue is PyReferenceExpression) {
            return getSpanNameFromReferenceExpression(assignedValue)
        }
    }

    return UNKNOWN_SPAN_NAME
}

@RequiresReadLock(generateAssertion = false)
fun getStringFromStringLiteralExpression(stringLiteralExpression: PyStringLiteralExpression): String {
    return stringLiteralExpression.stringValue
}


//receiver expression is the tracer in tracer.start_span()
@RequiresReadLock(generateAssertion = false)
fun getInstLibraryFromReceiverExpression(receiver: PyExpression): String {
    if (receiver is PyReferenceExpression) {
        return getInstLibraryFromReferenceExpression(receiver)
    } else if (receiver is PyCallExpression) {
        return getInstLibraryFromCallExpression(receiver)
    }

    return UNKNOWN_INST_LIBRARY
}

@RequiresReadLock(generateAssertion = false)
fun getInstLibraryFromReferenceExpression(referenceExpression: PyReferenceExpression): String {
    val psiElement = referenceExpression.reference.resolve()
    if (psiElement != null) {
        val context = psiElement.context
        if (context is PyAssignmentStatement) {
            return getInstLibraryFromAssignmentStatement(context)
        }
    }

    return UNKNOWN_INST_LIBRARY
}

@RequiresReadLock(generateAssertion = false)
fun getInstLibraryFromAssignmentStatement(assignmentStatement: PyAssignmentStatement): String {
    val assignedValue = assignmentStatement.assignedValue
    if (assignedValue is PyCallExpression) {
        return getInstLibraryFromCallExpression(assignedValue)
    }

    return UNKNOWN_INST_LIBRARY
}

@RequiresReadLock(generateAssertion = false)
fun getInstLibraryFromCallExpression(callExpression: PyCallExpression): String {
    val callee = callExpression.callee
    return callee?.reference?.let { reference ->
        val psiElement = reference.resolve()
        psiElement?.takeIf { it is PyFunction && it.name == OPENTELEMETRY_GET_TRACER_FUNC_NAME }?.let {
            val arguments = callExpression.arguments
            arguments.takeIf { it.isNotEmpty() }?.let { args ->
                val arg: PyExpression = args[0]
                getInstLibraryFromGetTracerArgument(arg)
            }
        }
    } ?: UNKNOWN_INST_LIBRARY
}

@RequiresReadLock(generateAssertion = false)
fun getInstLibraryFromGetTracerArgument(arg: PyExpression): String {
    if (arg is PyStringLiteralExpression) {
        return getStringFromStringLiteralExpression(arg)
    } else if (arg is PyReferenceExpression) {
        return getInstLibraryFromArgumentReferenceExpression(arg)
    }

    return UNKNOWN_INST_LIBRARY
}

@RequiresReadLock(generateAssertion = false)
fun getInstLibraryFromArgumentReferenceExpression(referenceExpression: PyReferenceExpression): String {
    if (PYTHON_MODULE_NAME_VARIABLE == referenceExpression.text) {
        return PYTHON_MODULE_NAME_VARIABLE
    }

    val pyElement = referenceExpression.reference.resolve()
    if (pyElement is PyTargetExpression) {
        val assignedValue = pyElement.findAssignedValue()
        if (assignedValue is PyStringLiteralExpression) {
            return getStringFromStringLiteralExpression(assignedValue)
        } else if (assignedValue is PyReferenceExpression) {
            return getInstLibraryFromArgumentReferenceExpression(assignedValue)
        }
    }

    return UNKNOWN_INST_LIBRARY
}

//the class index is not by qualified name, its short name index
//@RequiresReadLock(generateAssertion = false)
//fun findFunctionByClassNameAndFunctionName(
//    project: Project,
//    className: String,
//    functionName: String,
//    searchScopeProvider: GlobalSearchScopeProvider
//): PyFunction? {
//    val classes = PyClassNameIndex.find(className, project, searchScopeProvider.get())
//    for (pyClass in classes) {
//        val function = pyClass.findMethodByName(functionName, false, null)
//        if (function != null) {
//            return function
//        }
//    }
//    return null
//}

@RequiresReadLock(generateAssertion = false)
fun findFunctionByClassNameAndFunctionName(
    project: Project,
    className: String,
    functionName: String,
    searchScopeProvider: GlobalSearchScopeProvider
): PyFunction? {
    //the index will return a few functions, they have different containingClass, only one should match our search
    val functions = PyFunctionNameIndex.find(functionName, project, searchScopeProvider.get())
    return functions.firstOrNull { pyFunction: PyFunction -> pyFunction.isPhysical && pyFunction.containingClass?.qualifiedName == className }
}


@RequiresReadLock(generateAssertion = false)
fun findFunctionByMethodId(project: Project, methodId: String, searchScopeProvider: GlobalSearchScopeProvider): PyFunction? {
    return methodId.takeIf { isPythonMethodId(methodId) }?.let { id ->
        val methodName = getPythonMethodNameFromMethodId(id)
        PyFunctionNameIndex.find(methodName, project, searchScopeProvider.get())
            .firstOrNull { createPythonMethodCodeObjectId(project, it) == id }
    }


}