package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.environment.Env;
import org.digma.intellij.plugin.psi.LanguageServiceProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The central handler of EnvironmentChanged events.
 * it will perform the necessary actions that are common to all languages or IDEs.
 */
public class EnvironmentChangeHandler implements EnvironmentChanged {

    private final Logger logger = Logger.getInstance(this.getClass());

    private final Project project;

    public EnvironmentChangeHandler(Project project) {
        this.project = project;
    }

    //environmentChanged must run in a background thread.
    //when fired by the Environment object it is on background
    @Override
    public void environmentChanged(@Nullable Env newEnv) {

        //find any registered language service and call its environmentChanged method in case it has something to do
        // that is specific for that language.
        LanguageServiceProvider.getInstance(project).getLanguageServices().forEach(languageService -> {
            try {
                languageService.environmentChanged(newEnv);
            } catch (Throwable e) {
                Log.warnWithException(logger, e, "Exception in environmentChanged for languageService {}", languageService.getClass().getName());
                ErrorReporter.getInstance().reportError(project, "EnvironmentChangeHandler.environmentChanged", e);
            }
        });
    }


    @Override
    public void environmentsListChanged(List<Env> newEnvironments) {
        //nothing to do here
    }
}
