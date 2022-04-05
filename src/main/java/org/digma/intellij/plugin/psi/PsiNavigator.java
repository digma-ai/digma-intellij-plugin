package org.digma.intellij.plugin.psi;
import org.digma.intellij.plugin.psi.csharp.CSharpLanguageService;
import org.digma.intellij.plugin.psi.python.PythonLanguageService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.java.JavaLanguageService;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Query and navigate PSI elements.
 * This is the only class dealing with language specific services.
 */
public class PsiNavigator {

    enum Language {
        //todo: services can be also discovered with java service loader.
        // hard coded here has the advantage of clearly showing what we support.
        JAVA(JavaLanguageService.class),
        PYTHON(PythonLanguageService.class),
        CSHARP(CSharpLanguageService .class);

        private final Class<? extends LanguageService> serviceClass;

        Language(Class<? extends LanguageService> serviceClass) {
            this.serviceClass = serviceClass;
        }

        public Class<? extends LanguageService> getServiceClass() {
            return serviceClass;
        }
    }

    private static final Logger LOGGER = Logger.getInstance(PsiNavigator.class);
    private final EnumMap<Language, LanguageService> languageServices = new EnumMap<>(Language.class);
    private final NoOpLanguageService noOpLanguageService = new NoOpLanguageService();

    public PsiNavigator(Project project) {
        findAvailableLanguageServices(project);
    }

    private void findAvailableLanguageServices(Project project) {
        for (Language language : Language.values()) {
            var serviceClass = language.getServiceClass();
            var languageService = project.getService(serviceClass);
            if (languageService != null) {
                Log.log(LOGGER::debug, "found service {}", languageService);
                languageServices.put(language, languageService);
            }
        }
    }


    public boolean isInMethod(PsiElement psiElement) {
        return maybeGetMethod(psiElement) != null;
    }


    private PsiElement maybeGetMethod(PsiElement psiElement) {
        Log.log(LOGGER::debug, "in maybeGetMethod for element {}", psiElement);
        var languageService = findService(psiElement.getLanguage());
        Log.log(LOGGER::debug, "in maybeGetMethod found service {} for language {}", languageService, psiElement.getLanguage());
        var method = languageService.findParentMethodIfAny(psiElement);
        Log.log(LOGGER::debug, "in maybeGetMethod, got method? {}", method);
        return method;
    }


    @NotNull
    public PsiElement getMethod(PsiElement psiElement) {
        Log.log(LOGGER::debug, "in getMethod for element {}", psiElement);
        var languageService = findService(psiElement.getLanguage());
        Log.log(LOGGER::debug, "in getMethod found service {} for language {}", languageService, psiElement.getLanguage());
        var method = languageService.getParentMethod(psiElement);
        Log.log(LOGGER::debug, "in getMethod, got method? {}", method);
        return method;
    }

    @NotNull
    private LanguageService findService(com.intellij.lang.Language language) {
        Optional<LanguageService> optionalService = languageServices.values().stream()
                .filter(languageService -> languageService.accept(language))
                .findFirst();
        return optionalService.orElse(noOpLanguageService);
    }


    public boolean isSupportedFile(Project project, VirtualFile newFile) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(newFile);
        return languageServices.values().stream().anyMatch(t -> psiFile != null && t.accept(psiFile.getLanguage()));
    }


    private static class NoOpLanguageService implements LanguageService {

        @Override
        public boolean accept(com.intellij.lang.Language language) {
            return false;
        }

        @Override
        public PsiElement findParentMethodIfAny(PsiElement psiElement) {
            return null;
        }

        @Override
        public PsiElement getParentMethod(PsiElement psiElement) {
            throw new UnsupportedOperationException("we shouldn't be here with NoOp");
        }
    }
}
