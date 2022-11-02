package org.digma.intellij.plugin.emvironment;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.SupportedLanguages;

import java.util.List;

/**
 * The central handler of EnvironmentChanged events.
 * it will perform the necessary actions that are common to all languages or IDEs.
 */
public class EnvironmentChangeHandler implements EnvironmentChanged {


    private final Project project;

    private final DocumentInfoService documentInfoService;

    public EnvironmentChangeHandler(Project project) {
        this.project = project;
        documentInfoService = project.getService(DocumentInfoService.class);
    }

    //environmentChanged must run in a background thread.
    //when fired by the Environment object it is on background
    @Override
    public void environmentChanged(String newEnv) {

        //for all languages and IDEs documentInfoService needs to refresh its data
        documentInfoService.environmentChanged(newEnv);

        //find any registered language service and call its environmentChanged method in case it has something to do
        // that is specific for that language.
        for (SupportedLanguages value : SupportedLanguages.values()) {

            try {
                @SuppressWarnings("unchecked") // the unchecked cast should be ok here
                Class<? extends LanguageService> clazz = (Class<? extends LanguageService>) Class.forName(value.getLanguageServiceClassName());
                LanguageService languageService = project.getService(clazz);
                if (languageService != null) {
                    languageService.environmentChanged(newEnv);
                }

            } catch (Throwable e) {
                //ignore: some classes will fail to load , for example the CSharpLanguageService
                //will fail to load if it's not rider because it depends on rider classes.
                //don't log, it will happen too many times
            }
        }
    }

    @Override
    public void environmentsListChanged(List<String> newEnvironments) {
        //nothing to do here
    }
}
