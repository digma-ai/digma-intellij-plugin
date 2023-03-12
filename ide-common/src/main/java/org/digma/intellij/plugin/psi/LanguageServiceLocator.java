package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class LanguageServiceLocator {

    private static final Logger LOGGER = Logger.getInstance(LanguageServiceLocator.class);

    private final Project project;

    private final Cache cache = new Cache();

    public LanguageServiceLocator(Project project) {
        this.project = project;
    }

    public static LanguageServiceLocator getInstance(Project project){
        return project.getService(LanguageServiceLocator.class);
    }


    @NotNull
    public LanguageService locate(@Nullable Language language) {

        if (language == null){
            return NoOpLanguageService.INSTANCE;
        }

        if (cache.contains(language)) {
            return cache.get(language);
        }

        for (SupportedLanguages value : SupportedLanguages.values()) {

            try {
                @SuppressWarnings("unchecked")
                Class<? extends LanguageService> clazz = (Class<? extends LanguageService>) Class.forName(value.getLanguageServiceClassName());
                LanguageService languageService = project.getService(clazz);
                if (languageService != null && languageService.isServiceFor(language)) {
                    cache.put(language, languageService);
                    return languageService;
                }

            } catch (Exception e) {
                //catch Throwable because there may be errors.
                //ignore: some classes will fail to load , for example the CSharpLanguageService
                //will fail to load if it's not rider because it depends on rider classes.
                //don't log, it will happen too many times
            }
        }

        Log.log(LOGGER::debug, "Could not find language service for language: {}, Using no-op language service", language.getID());

        /*
            cases when NoOpLanguageService will be used:
            when the language is not supported.
            when the language service was not registered. for example opening a python file in rider and the python
            plugin is not installed in the IDE, in that case we also don't register the python language service.
         */

        return NoOpLanguageService.INSTANCE;

    }


    private static class Cache {

        private final Map<String, LanguageService> languageServiceMap = new HashMap<>();

        void put(@NotNull Language language, @NotNull LanguageService languageService) {
            languageServiceMap.put(language.getID(), languageService);
        }

        LanguageService get(Language language) {
            return languageServiceMap.get(language.getID());
        }

        boolean contains(Language language) {
            return languageServiceMap.containsKey(language.getID());
        }

    }

}
