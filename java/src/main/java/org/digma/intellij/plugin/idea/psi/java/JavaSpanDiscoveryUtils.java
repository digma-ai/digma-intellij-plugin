package org.digma.intellij.plugin.idea.psi.java;


import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.digma.intellij.plugin.idea.psi.java.Constants.GLOBAL_OPENTELEMETY_FQN;
import static org.digma.intellij.plugin.idea.psi.java.Constants.OPENTELEMETY_FQN;
import static org.digma.intellij.plugin.idea.psi.java.Constants.SPAN_BUILDER_FQN;
import static org.digma.intellij.plugin.idea.psi.java.Constants.TRACER_BUILDER_FQN;
import static org.digma.intellij.plugin.idea.psi.java.Constants.TRACER_FQN;
import static org.digma.intellij.plugin.idea.psi.java.Constants.WITH_SPAN_INST_LIBRARY_1;
import static org.digma.intellij.plugin.idea.psi.java.Constants.WITH_SPAN_INST_LIBRARY_2;
import static org.digma.intellij.plugin.idea.psi.java.Constants.WITH_SPAN_INST_LIBRARY_3;
import static org.digma.intellij.plugin.idea.psi.java.Constants.WITH_SPAN_INST_LIBRARY_4;
import static org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils.createJavaMethodCodeObjectId;
import static org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils.createSpanIdForWithSpanAnnotation;
import static org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils.createSpanIdFromInstLibraryAndSpanName;
import static org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils.createSpanNameForWithSpanAnnotation;
import static org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils.getValueFromFirstArgument;
import static org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils.isMethodWithFirstArgumentString;
import static org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils.isMethodWithNoArguments;

/**
 * Utility methods for span discovery.
 * all of those methods must be called in the scope of a ReadAction.
 */
@SuppressWarnings("UnstableApiUsage")
public class JavaSpanDiscoveryUtils {
    private JavaSpanDiscoveryUtils() {
    }

    /*
    most methods here are private and should stay private.
    the code is split to smaller methods for clarity, but they should run as a whole flow because some methods relay on
    checks made by previous methods.
     */


    /**
     * Creates a SpanInfo from a @WithSpan annotated method.
     * this method should be called with psiMethod that was validated to be annotated with @WithSpan otherwise it
     * will return null.
     */
    @Nullable
    public static List<SpanInfo> getSpanInfoFromWithSpanAnnotatedMethod(@NotNull PsiMethod psiMethod) {

        var withSpanAnnotation = psiMethod.getAnnotation(Constants.WITH_SPAN_FQN);
        var containingClass = psiMethod.getContainingClass();
        PsiFile containingFile = PsiTreeUtil.getParentOfType(psiMethod, PsiFile.class);

        //withSpanAnnotation,containingFile and containingClass must not be null because we found this annotation in a search.
        // a method in java must have a containing class. (psiMethod.getContainingClass may return null because
        // it supports other languages like groovy and kotlin)
        if (withSpanAnnotation != null && containingFile != null && containingClass != null) {

            var methodId = createJavaMethodCodeObjectId(psiMethod);
            Objects.requireNonNull(methodId, "methodId must not be null here");
            var containingFileUri = PsiUtils.psiFileToUri(containingFile);
            Objects.requireNonNull(containingFileUri, "containingFileUri must not be null here");

            var spanName = createSpanNameForWithSpanAnnotation(psiMethod, withSpanAnnotation, containingClass);

            List<SpanInfo> spanInfos = new ArrayList<>();
            spanInfos.add(new SpanInfo(createSpanIdForWithSpanAnnotation(psiMethod, withSpanAnnotation, containingClass,WITH_SPAN_INST_LIBRARY_1), spanName, methodId, containingFileUri));
            spanInfos.add(new SpanInfo(createSpanIdForWithSpanAnnotation(psiMethod, withSpanAnnotation, containingClass,WITH_SPAN_INST_LIBRARY_2), spanName, methodId, containingFileUri));
            spanInfos.add(new SpanInfo(createSpanIdForWithSpanAnnotation(psiMethod, withSpanAnnotation, containingClass,WITH_SPAN_INST_LIBRARY_3), spanName, methodId, containingFileUri));
            spanInfos.add(new SpanInfo(createSpanIdForWithSpanAnnotation(psiMethod, withSpanAnnotation, containingClass,WITH_SPAN_INST_LIBRARY_4), spanName, methodId, containingFileUri));
            return spanInfos;
        }

        //if here then we couldn't completely discover the span
        return null;
    }


    /**
     * Create a SpanInfo from a reference to io.opentelemetry.api.trace.SpanBuilder.startSpan method.
     * this method should be called with reference that was validated to be a reference to startSpan otherwise it will return null.
     */
    @Nullable
    public static SpanInfo getSpanInfoFromStartSpanMethodReference(@NotNull Project project, @NotNull PsiReference startSpanMethodReference) {

        //validate that it's a reference to startSpan method
        PsiElement startSpanMethod = startSpanMethodReference.resolve();
        if (startSpanMethod == null || !isMethodWithNoArguments(startSpanMethod, "startSpan", SPAN_BUILDER_FQN)) {
            return null;
        }

        if (startSpanMethodReference instanceof PsiReferenceExpression) {

            String methodId;
            String containingFileUri;

            PsiMethod containingMethod = PsiTreeUtil.getParentOfType(startSpanMethodReference.getElement(), PsiMethod.class);
            PsiFile containingFile = startSpanMethodReference.getElement().getContainingFile();
            //span is not relevant if there is no containing method. one can build a Span as a class member, we don't support that.
            if (containingMethod != null && containingFile != null) {
                methodId = createJavaMethodCodeObjectId(containingMethod);
                Objects.requireNonNull(methodId, "methodId must not be null here");
                containingFileUri = PsiUtils.psiFileToUri(containingFile);
                Objects.requireNonNull(containingFileUri, "containingFileUri must not be null here");

                //navigate the psi tree to find the call to spanBuilder method where we need to take the span name from.
                //qualifier expression of startSpanMethodReference should be the spanBuilder("span name") method call,
                // or a variable created by spanBuilder("span name").
                //startSpan method must have a qualifier.
                PsiExpression spanBuilderReferenceExpression = ((PsiReferenceExpression) startSpanMethodReference).getQualifierExpression();
                if (spanBuilderReferenceExpression instanceof PsiMethodCallExpression) {
                    return getSpanInfoFromSpanBuilderMethodCallExpression(project, (PsiMethodCallExpression) spanBuilderReferenceExpression, methodId, containingFileUri);
                } else if (spanBuilderReferenceExpression instanceof PsiReferenceExpression) {
                    return getSpanInfoFromSpanBuilderReferenceExpression(project, (PsiReferenceExpression) spanBuilderReferenceExpression, methodId, containingFileUri);
                }
            }
        }

        //if here then we couldn't completely discover the span
        return null;
    }


    @Nullable
    private static SpanInfo getSpanInfoFromSpanBuilderReferenceExpression(@NotNull Project project, @NotNull PsiReferenceExpression spanBuilderReferenceExpression, @NotNull String methodId, @NotNull String containingFileUri) {
        /*
        for example spanBuilderReferenceExpression would be the spanBuilder variable,
        or a class member:
        SpanBuilder spanBuilder = tracer.spanBuilder("MySpanName");
        Span span = spanBuilder.startSpan();
         */

        PsiElement spanBuilderReference = spanBuilderReferenceExpression.resolve();
        if (spanBuilderReference instanceof PsiVariable) {
            return getSpanInfoFromSpanBuilderVariable(project, (PsiVariable) spanBuilderReference, methodId, containingFileUri);
        }

        return null;
    }


    @Nullable
    private static SpanInfo getSpanInfoFromSpanBuilderVariable(@NotNull Project project,@NotNull  PsiVariable spanBuilderVariable, @NotNull String methodId, @NotNull String containingFileUri) {

        //search references to the variable, if an assignment is found use it to find the SpanInfo.
        //if no assignment is found use the variable initialization

        Query<PsiReference> spanBuilderReferences = ReferencesSearch.search(spanBuilderVariable, GlobalSearchScope.projectScope(project));
        //find the first assignment to spanBuilder,if there are few assignments we take the first one, it may be incorrect from
        //the programmer's point of view, but we don't have an idea which one to take unless we do much more static analysis.
        PsiReference spanBuilderAssignmentReference = spanBuilderReferences.filtering(psiReference ->
                psiReference instanceof PsiReferenceExpression &&
                        ((PsiReferenceExpression) psiReference).getParent() instanceof PsiAssignmentExpression).findFirst();


        if (spanBuilderAssignmentReference instanceof PsiReferenceExpression) {
            PsiElement spanBuilderAssignmentExpression = ((PsiReferenceExpression) spanBuilderAssignmentReference).getParent();
            if (spanBuilderAssignmentExpression instanceof PsiAssignmentExpression) {
                return getSpanInfoFromSpanBuilderAssignmentExpression(project, (PsiAssignmentExpression) spanBuilderAssignmentExpression, methodId, containingFileUri);
            }
        } else {
            //initializer is : this.myMemberSpanBuilder = tracer.spanBuilder("xxx")
            PsiExpression initializer = spanBuilderVariable.getInitializer();
            if (initializer instanceof PsiMethodCallExpression) {
                return getSpanInfoFromSpanBuilderMethodCallExpression(project, (PsiMethodCallExpression) initializer, methodId, containingFileUri);
            }else if(initializer instanceof PsiReferenceExpression){
                return getSpanInfoFromSpanBuilderReferenceExpression(project, (PsiReferenceExpression) initializer,methodId,containingFileUri);
            }
        }

        return null;
    }




    @Nullable
    private static SpanInfo getSpanInfoFromSpanBuilderAssignmentExpression(@NotNull Project project,@NotNull  PsiAssignmentExpression spanBuilderAssignmentExpression, @NotNull String methodId, @NotNull String containingFileUri) {

        PsiExpression rightAssignmentExpression = spanBuilderAssignmentExpression.getRExpression();
        if (rightAssignmentExpression instanceof PsiMethodCallExpression) {
            return getSpanInfoFromSpanBuilderMethodCallExpression(project, (PsiMethodCallExpression) rightAssignmentExpression, methodId, containingFileUri);
        }
        return null;
    }


    @Nullable
    private static SpanInfo getSpanInfoFromSpanBuilderMethodCallExpression(@NotNull Project project, @NotNull PsiMethodCallExpression spanBuilderMethodCall, @NotNull String methodId, @NotNull String containingFileUri) {

        //this is the method call to tracer.spanBuilder("span name")

        //resolve spanBuilder("span name") method and validate that it's the method we're looking for
        PsiElement spanBuilderMethod = spanBuilderMethodCall.getMethodExpression().resolve();
        boolean isSpanBuilderMethod = spanBuilderMethod != null &&
                isMethodWithFirstArgumentString(spanBuilderMethod, "spanBuilder", TRACER_FQN);
        //continue only if spanBuilderMethod is a PsiMethod and is the method we're looking for
        if (!isSpanBuilderMethod) {
            return null;
        }

        String spanName;
        String instLibrary = null;

        //the span name expression is always the first argument
        spanName = getValueFromFirstArgument(spanBuilderMethodCall.getArgumentList());
        //continue only if we have a span name
        if (spanName != null) {
            //qualifier expression should be the tracer object reference in tracer.spanBuilder("pet validate")
            PsiExpression tracerReferenceExpression = spanBuilderMethodCall.getMethodExpression().getQualifierExpression();
            if (tracerReferenceExpression instanceof PsiReferenceExpression) {
                instLibrary = getInstLibraryFromTracerReferenceExpression(project, (PsiReferenceExpression) tracerReferenceExpression);
            } else if (tracerReferenceExpression instanceof PsiMethodCallExpression) {
                instLibrary = getInstLibraryFromMethodCallExpression(project, (PsiMethodCallExpression) tracerReferenceExpression);
            }
        }

        if (spanName != null && instLibrary != null) {
            String spanId = createSpanIdFromInstLibraryAndSpanName(instLibrary, spanName);
            return new SpanInfo(spanId, spanName, methodId, containingFileUri);
        }

        //if here then we couldn't completely discover the span
        return null;
    }


    @Nullable
    private static String getInstLibraryFromTracerReferenceExpression(@NotNull Project project, @NotNull PsiReferenceExpression tracerReferenceExpression) {

        //tracerReferenceExpression is the tracer object in tracer.spanBuilder("pet validate")

        //find the declaration of the tracer object, it may be a class member or local variable
        PsiElement tracerReference = tracerReferenceExpression.resolve();
        if (tracerReference instanceof PsiVariable) {
            return getInstLibraryFromTracerPsiVariable(project, (PsiVariable) tracerReference);
        }

        return null;
    }


    @Nullable
    private static String getInstLibraryFromTracerPsiVariable(@NotNull Project project, @NotNull PsiVariable tracerVariable) {
        //search references to the variable, if an assignment is found use it to fine the inst library.
        //if no assignment is found use the variable initialization
        Query<PsiReference> tracerReferences = ReferencesSearch.search(tracerVariable, GlobalSearchScope.projectScope(project));
        //find the first assignment to tracer,if there are few assignments we take the first one, it may be incorrect from
        //the programmer's point of view, but we don't have an idea which one to take unless we do much more static analysis.
        PsiReference tracerAssignmentReference = tracerReferences.filtering(psiReference ->
                psiReference instanceof PsiReferenceExpression &&
                        ((PsiReferenceExpression) psiReference).getParent() instanceof PsiAssignmentExpression).findFirst();


        if (tracerAssignmentReference instanceof PsiReferenceExpression) {
            PsiElement tracerAssignmentExpression = ((PsiReferenceExpression) tracerAssignmentReference).getParent();
            if (tracerAssignmentExpression instanceof PsiAssignmentExpression) {
                return getInstLibraryFromTracerAssignmentExpression(project, (PsiAssignmentExpression) tracerAssignmentExpression);
            }
        } else {
            //initializer is : private Tracer tracer = openTelemetry.getTracer("MyTestTracer");
            PsiExpression initializer = tracerVariable.getInitializer();
            if (initializer instanceof PsiMethodCallExpression) {
                return getInstLibraryFromMethodCallExpression(project, (PsiMethodCallExpression) initializer);
            }
        }

        return null;
    }


    @Nullable
    private static String getInstLibraryFromTracerAssignmentExpression(@NotNull Project project, @NotNull PsiAssignmentExpression tracerAssignmentExpression) {

        //tracerAssignmentExpression is this for example: this.tracer = openTelemetry.getTracer("MyTestTracer");
        PsiExpression rightAssignmentExpression = tracerAssignmentExpression.getRExpression();
        if (rightAssignmentExpression instanceof PsiMethodCallExpression) {
            return getInstLibraryFromMethodCallExpression(project, (PsiMethodCallExpression) rightAssignmentExpression);
        }

        return null;
    }


    @Nullable
    private static String getInstLibraryFromMethodCallExpression(@NotNull Project project, @NotNull PsiMethodCallExpression tracerMethodCallExpression) {
        PsiMethod getTracerMethod = (PsiMethod) tracerMethodCallExpression.getMethodExpression().resolve();
        if (getTracerMethod != null) {
            if (isMethodWithFirstArgumentString(getTracerMethod, "getTracer", OPENTELEMETY_FQN) ||
                    isMethodWithFirstArgumentString(getTracerMethod, "getTracer", GLOBAL_OPENTELEMETY_FQN)) {
                return getValueFromFirstArgument(tracerMethodCallExpression.getArgumentList());
            } else if (isMethodWithNoArguments(getTracerMethod, "build", TRACER_BUILDER_FQN)) {
                PsiExpression tracerBuilderBuildMethodQualifier = tracerMethodCallExpression.getMethodExpression().getQualifierExpression();
                if (tracerBuilderBuildMethodQualifier instanceof PsiMethodCallExpression) {
                    return getInstLibraryFromMethodCallExpression(project, (PsiMethodCallExpression) tracerBuilderBuildMethodQualifier);
                }else if(tracerBuilderBuildMethodQualifier instanceof PsiReferenceExpression){
                    return getInstLibraryFromTracerReferenceExpression(project, (PsiReferenceExpression) tracerBuilderBuildMethodQualifier);
                }
            } else if (isMethodWithFirstArgumentString(getTracerMethod, "tracerBuilder", OPENTELEMETY_FQN)) {
                return getValueFromFirstArgument(tracerMethodCallExpression.getArgumentList());
            }
        }
        return null;
    }


    @NotNull
    public static Query<PsiReference> filterNonRelevantReferencesForSpanDiscovery(@NotNull Query<PsiReference> psiReferences) {
        return psiReferences.filtering(psiReference -> {
            var aClass = PsiTreeUtil.getParentOfType(psiReference.getElement(), PsiClass.class);
            return aClass == null ||
                    (!aClass.isAnnotationType() && !aClass.isEnum() && !aClass.isRecord());
        });
    }

    @NotNull
    public static Query<PsiMethod> filterNonRelevantMethodsForSpanDiscovery(@NotNull Query<PsiMethod> psiMethods) {
        return psiMethods.filtering(psiMethod -> {
            var file = PsiTreeUtil.getParentOfType(psiMethod,PsiFile.class);
            //only java files are relevant
            if (file instanceof PsiJavaFile) {
                var aClass = PsiTreeUtil.getParentOfType(psiMethod, PsiClass.class);
                return aClass == null ||
                        (!aClass.isAnnotationType() && !aClass.isEnum() && !aClass.isRecord());
            }
            return false;
        });
    }
}
