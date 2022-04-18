package org.digma.intellij.plugin.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.csharp.CSharpLanguageService;
import org.digma.intellij.plugin.psi.java.JavaLanguageService;
import org.digma.intellij.plugin.psi.python.PythonLanguageService;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Query and navigate PSI elements.
 * This is the only class dealing with language specific services.
 */
public class DigmaPsiManager {

    enum Language {
        //todo: services can be also discovered with java service loader.
        // hard coded here has the advantage of clearly showing what we support.
        JAVA(JavaLanguageService.class),
        PYTHON(PythonLanguageService.class),
        CSHARP(CSharpLanguageService.class);

        private final Class<? extends LanguageService> serviceClass;

        Language(Class<? extends LanguageService> serviceClass) {
            this.serviceClass = serviceClass;
        }

        public Class<? extends LanguageService> getServiceClass() {
            return serviceClass;
        }
    }

    private static final Logger LOGGER = Logger.getInstance(DigmaPsiManager.class);

    private final EnumMap<Language, LanguageService> languageServices = new EnumMap<>(Language.class);
    private final NoOpLanguageService noOpLanguageService = new NoOpLanguageService();

    public DigmaPsiManager(Project project) {
        findAvailableLanguageServices(project);
    }

    //not all services are available, we install services suitable for the current IDE.
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


    @Nullable
    public MethodIdentifier detectMethodUnderCaret(Project project, int caretOffset, VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        LanguageService languageService = findService(psiFile.getLanguage());
        return languageService.detectMethodUnderCaret(project, psiFile, caretOffset);
    }


    private static class NoOpLanguageService implements LanguageService {
        @Override
        public boolean accept(com.intellij.lang.Language language) {
            return false;
        }

        @Override
        @Nullable
        public MethodIdentifier detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
            return null;
        }
    }
}
