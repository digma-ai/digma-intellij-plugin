package org.digma.intellij.plugin.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.java.JavaLanguageService;
import org.digma.intellij.plugin.psi.python.PythonLanguageService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Query and navigate PSI elements.
 * This is the only class dealing with language specific elements.
 */
public class PsiNavigator {

    enum Language {
        JAVA(JavaLanguageService.class),
        PYTHON(PythonLanguageService.class);

        private final Class<? extends LanguageService> serviceClass;

        Language(Class<? extends LanguageService> serviceClass) {
            this.serviceClass = serviceClass;
        }

        public Class<? extends LanguageService> getServiceClass() {
            return serviceClass;
        }
    }

    private static final Logger LOGGER = Logger.getInstance(PsiNavigator.class);
    private final Map<Language,LanguageService> languageServices = new HashMap<>();
    private final NoOpLanguageService noOpLanguageService = new NoOpLanguageService();

    public PsiNavigator(Project project) {
        findAvailableLanguageServices(project);
    }

    private void findAvailableLanguageServices(Project project) {
        for(Language language: Language.values()){
            Class<? extends LanguageService> serviceClass = language.getServiceClass();
            LanguageService languageService = project.getService(serviceClass);
            if (languageService != null){
                Log.log(LOGGER::debug, "found service {}",languageService);
                languageServices.put(language,languageService);
            }
        }
    }



    public boolean isInMethod(PsiElement psiElement) {
        return maybeGetMethod(psiElement) != null;
    }


    //todo: can return null
    private PsiElement maybeGetMethod(PsiElement psiElement) {
        return getMethod(psiElement);
    }


    //todo: can not return null
    public PsiElement getMethod(PsiElement psiElement) {
        Log.log(LOGGER::debug, "in getMethod for element {}", psiElement);
        LanguageService languageService = findService(psiElement.getLanguage());
        Log.log(LOGGER::debug, "in getMethod found service {} for language {}", languageService,psiElement.getLanguage());
        PsiElement method = languageService.getMethod(psiElement);
        Log.log(LOGGER::debug, "in getMethod, got method? {}", method);
        return method;
    }

    private LanguageService findService(com.intellij.lang.Language language) {
        Optional<LanguageService> optionalService = languageServices.values().stream()
                .filter(languageService -> languageService.accept(language))
                .findFirst();
        return optionalService.orElse(noOpLanguageService);
    }


}
