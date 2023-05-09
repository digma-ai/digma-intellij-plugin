package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.lang.jvm.JvmMethod;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;


/**
 * A collection of reusable utility methods for the java language, mostly PSI related.
 * most if not all of these methods must be called in the scope of a ReadAction.
 */
@SuppressWarnings("UnstableApiUsage")
public class JavaLanguageUtils {
    private JavaLanguageUtils() {
    }

    @NotNull
    public static String createJavaMethodCodeObjectId(@NotNull PsiMethod method) {

        //usually these should be not null for java. but the PSI api declares them as null, so we must check.
        // they can be null for groovy/kotlin
        if (method.getContainingClass() == null || method.getContainingClass().getQualifiedName() == null) {
            return method.getName();
        }

        var packageName = ((PsiJavaFile) method.getContainingFile()).getPackageName();

        var className = "";
        try {
            className = method.getContainingClass().getQualifiedName().substring(packageName.length() + 1).replace('.', '$');
        } catch (NullPointerException e) {
            //there should not be a NPE for java method because in java a method must have a containing class.
            // It's only to satisfy intellij warnings.
            //the methods getContainingClass and getQualifiedName may return null, but it can only happen
            //for other jvm languages like scala/groovy/kotlin
        }

        return packageName + "." + className + "$_$" + method.getName();
    }


    @NotNull
    public static String createSpanNameForWithSpanAnnotation(@NotNull PsiMethod psiMethod, @NotNull PsiAnnotation withSpanAnnotation, @NotNull PsiClass containingClass) {
        //must be called in ReadAction and smart mode
        var value = getPsiAnnotationAttributeValue(withSpanAnnotation, "value");
        if (value != null && !value.isBlank()) {
            return value;
        } else {
            return containingClass.getName() + "." + psiMethod.getName();
        }
    }


    @NotNull
    public static String createSpanIdForWithSpanAnnotation(@NotNull PsiMethod psiMethod, @NotNull PsiAnnotation withSpanAnnotation, @NotNull PsiClass containingClass, @NotNull String instLibrary) {
        var spanName = createSpanNameForWithSpanAnnotation(psiMethod, withSpanAnnotation, containingClass);
        return instLibrary + "$_$" + spanName;
    }

    @NotNull
    public static String createSpanIdFromInstLibraryAndSpanName(@NotNull String instLibrary, @NotNull String spanName) {
        return instLibrary + "$_$" + spanName;
    }


    @Nullable
    public static String getPsiAnnotationAttributeValue(@NotNull PsiAnnotation annotation, @NotNull String attributeName) {
        var attrValue = annotation.findAttributeValue(attributeName);
        return extractValue(attrValue);
    }

    @NotNull
    public static List<String> getPsiAnnotationAttributeValues(@NotNull PsiAnnotation annotation, @NotNull String attributeName) {
        var attrValue = annotation.findAttributeValue(attributeName);
        List<PsiAnnotationMemberValue> sourceList;
        if (attrValue instanceof PsiArrayInitializerMemberValue) {
            PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValue) attrValue).getInitializers();
            sourceList = Arrays.stream(initializers).toList();
        } else {
            sourceList = List.of(attrValue);
        }

        List<String> retList = sourceList.stream()
                .map(it -> extractValue(it))
                .filter(it -> it != null)
                .toList();
        return retList;
    }

    @Nullable
    private static String extractValue(@NotNull PsiAnnotationMemberValue value) {
        if (value instanceof PsiLiteral) {
            return getPsiLiteralValue((PsiLiteral) value);
        } else if (value instanceof PsiExpression) {
            return getPsiExpressionValue((PsiExpression) value);
        }
        return null;
    }


    @Nullable
    public static String getPsiLiteralValue(@NotNull PsiLiteral psiLiteral) {
        if (psiLiteral.getValue() != null) {
            return psiLiteral.getValue().toString();
        }
        return null;
    }


    @Nullable
    public static String getPsiExpressionValue(@NotNull PsiExpression expression) {
        if (expression instanceof PsiReferenceExpression) {
            return getPsiReferenceExpressionValue((PsiReferenceExpression) expression);
        } else if (expression instanceof PsiLiteralExpression) {
            return getPsiLiteralValue((PsiLiteralExpression) expression);
        }
        return null;
    }


    @Nullable
    public static String getPsiReferenceExpressionValue(@NotNull PsiReferenceExpression psiReferenceExpression) {
        var element = psiReferenceExpression.resolve();
        if (element instanceof PsiField) {
            return getPsiFieldValue((PsiField) element);
        } else if (element instanceof PsiLocalVariable) {
            return getPsiLocalVariableValue((PsiLocalVariable) element);
        }

        return null;
    }


    @Nullable
    private static String getPsiLocalVariableValue(@NotNull PsiLocalVariable localVariable) {
        PsiElement initializer = localVariable.getInitializer();
        if (initializer instanceof PsiLiteralExpression) {
            return getPsiLiteralValue((PsiLiteral) initializer);
        } else if (initializer instanceof PsiReferenceExpression) {
            return getPsiReferenceExpressionValue((PsiReferenceExpression) initializer);
        }
        return null;
    }


    @Nullable
    public static String getPsiFieldValue(@NotNull PsiField psiField) {
        var initializer = psiField.getInitializer();
        if (initializer instanceof PsiLiteral) {
            return getPsiLiteralValue((PsiLiteral) initializer);
        } else if (initializer instanceof PsiReferenceExpression) {
            return getPsiReferenceExpressionValue((PsiReferenceExpression) initializer);
        }
        return psiField.getText();
    }


    @Nullable
    public static PsiMethod findMethodInClass(@NotNull PsiClass psiClass, @NotNull String methodName, @NotNull Predicate<PsiMethod> methodPredicate) {
        var methods = psiClass.findMethodsByName(methodName);
        for (JvmMethod method : methods) {
            if (method instanceof PsiMethod && methodPredicate.test((PsiMethod) method)) {
                return (PsiMethod) method;
            }
        }
        return null;
    }


    @Nullable
    public static String getValueOfFirstMatchingAnnotationAttribute(@NotNull PsiAnnotation psiAnnotation, @NotNull List<String> attributeList, @Nullable String defaultValue) {
        List<JvmAnnotationAttribute> annotationAttributes = psiAnnotation.getAttributes();

        for (JvmAnnotationAttribute curr : annotationAttributes) {
            if (attributeList.contains(curr.getAttributeName())) {
                String theValue = JavaLanguageUtils.getPsiAnnotationAttributeValue(psiAnnotation, curr.getAttributeName());
                return theValue;
            }
        }
        return defaultValue;
    }

    @NotNull
    public static List<String> getValuesOfFirstMatchingAnnotationAttribute(@NotNull PsiAnnotation psiAnnotation, @NotNull List<String> attributeList) {
        List<JvmAnnotationAttribute> annotationAttributes = psiAnnotation.getAttributes();

        for (JvmAnnotationAttribute curr : annotationAttributes) {
            if (attributeList.contains(curr.getAttributeName())) {
                return JavaLanguageUtils.getPsiAnnotationAttributeValues(psiAnnotation, curr.getAttributeName());
            }
        }
        return Collections.emptyList();
    }

    /**
     * test if the psiElement is a method with methodName and containingClassName.
     * methodPredicate can be used to test more properties of the method like its access modifiers and arguments.
     */
    public static boolean isMethod(@NotNull PsiElement psiElement, @NotNull String methodName, @NotNull String containingClassName, @Nullable Predicate<PsiMethod> methodPredicate) {

        if (psiElement instanceof PsiMethod &&
                ((PsiMethod) psiElement).getName().equals(methodName) &&
                ((PsiMethod) psiElement).getContainingClass() != null &&
                ((PsiMethod) psiElement).getContainingClass().getQualifiedName() != null &&
                ((PsiMethod) psiElement).getContainingClass().getQualifiedName().equals(containingClassName)) {

            return methodPredicate == null || methodPredicate.test((PsiMethod) psiElement);

        }

        return false;
    }

    public static boolean isMethodWithFirstArgumentString(@NotNull PsiElement psiElement, @NotNull String methodName, @NotNull String containingClassName) {
        return isMethod(psiElement, methodName, containingClassName, psiMethod -> {
            if (psiMethod.getParameters().length >= 1 && psiMethod.getParameters()[0].getType() instanceof PsiClassReferenceType) {
                var type = ((PsiClassReferenceType) psiMethod.getParameters()[0].getType()).resolve();
                if (type != null) {
                    return type.getQualifiedName() != null && type.getQualifiedName().equals("java.lang.String");
                }
            }
            return false;
        });
    }

    public static boolean isMethodWithNoArguments(@NotNull PsiElement psiElement, @NotNull String methodName, @NotNull String containingClassName) {
        return isMethod(psiElement, methodName, containingClassName, psiMethod -> psiMethod.getParameters().length == 0);
    }


    @Nullable
    public static String getValueFromFirstArgument(@NotNull PsiExpressionList argumentList) {
        PsiExpression[] psiExpressions = argumentList.getExpressions();
        if (psiExpressions.length >= 1) {
            PsiExpression expression = psiExpressions[0];
            return getPsiExpressionValue(expression);
        }

        return null;
    }


}
