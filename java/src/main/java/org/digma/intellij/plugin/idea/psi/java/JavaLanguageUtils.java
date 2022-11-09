package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.psi.*;

public class JavaLanguageUtils {

    public static final String WITH_SPAN_FQN = "io.opentelemetry.instrumentation.annotations.WithSpan";
    public static final String WITH_SPAN_INST_LIBRARY = "io.opentelemetry.spring-boot-autoconfigure";


    public static String createJavaMethodCodeObjectId(PsiMethod method) {

        //usually these should be not null for java. but the PSI api declares them as null so we must check.
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


    public static String createWithSpanAnnotationSpanName(PsiMethod psiMethod, PsiAnnotation withSpanAnnotation, PsiClass containingClass) {
        //must be called in ReadAction and smart mode
        var value = JavaLanguageUtils.getPsiAnnotationAttributeValue(withSpanAnnotation,"value");
        var name = containingClass.getName() + "." + psiMethod.getName() + value;
        if (value != null && !value.isBlank()){
            name = name + "." + value;
        }
        return name;
    }


    public static String createWithSpanAnnotationCodeObjectId(PsiMethod psiMethod, PsiAnnotation withSpanAnnotation, PsiClass containingClass){
        var spanName = createWithSpanAnnotationSpanName(psiMethod,withSpanAnnotation,containingClass);
        return WITH_SPAN_INST_LIBRARY + "$_$" + spanName;
    }




    public static String getPsiAnnotationAttributeValue(PsiAnnotation annotation, String attributeName){

        var value = annotation.findAttributeValue(attributeName);

        if (value instanceof PsiLiteral){
            return getPsiLiteralValue((PsiLiteral) value);
        }else if (value instanceof PsiReferenceExpression){
            return getPsiReferenceExpressionValue((PsiReferenceExpression) value);
        }

        return null;
    }


    public static String getPsiLiteralValue(PsiLiteral psiLiteral){
        if (psiLiteral.getValue() != null){
            return psiLiteral.getValue().toString();
        }
        return null;
    }

    public static String getPsiReferenceExpressionValue(PsiReferenceExpression psiReferenceExpression){
        var element = psiReferenceExpression.resolve();
        if (element instanceof PsiField){
            return getPsiFieldValue((PsiField) element);
        }

        return null;
    }


    public static String getPsiFieldValue(PsiField psiField){
        var initializer = psiField.getInitializer();
        if (initializer instanceof PsiLiteral){
            return getPsiLiteralValue((PsiLiteral) initializer);
        }
        return null;
    }

}
