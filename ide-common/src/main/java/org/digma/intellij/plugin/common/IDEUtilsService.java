package org.digma.intellij.plugin.common;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.psi.*;
import org.jetbrains.annotations.*;

/*
This class is not perfect but it does serve our needs.
see the comments in com.intellij.util.PlatformUtils, PlatformUtils is marked internal API so its better not to user it.
for what we need its probably ok:
isIdeaIDE() is for sure Idea if we have JavaLanguageService installed.
isRiderIDE() is for sure rider if we have CHarpLanguageService installed.
isPyCharmIDE() is for sure pycharm if we have PythonLanguageService installed and it's not Idea.
if we need more capabilities here we need to explore the technics suggested in com.intellij.util.PlatformUtils
 */

@Service(Service.Level.PROJECT)
public final class IDEUtilsService {

    private final Idea idea;
    private final Rider rider;
    private final Pycharm pycharm;


    public IDEUtilsService(Project project) {
        idea = new Idea(project);
        rider = new Rider(project);
        pycharm = new Pycharm(project);
    }

    public static IDEUtilsService getInstance(@NotNull Project project){
        return project.getService(IDEUtilsService.class);
    }


    /*
    There is no easy way to know if a project is a java project. intellij doesn't have a project type. intellij projects
    have modules and each module has some nature. there may be a project that contains a java module and python module,
    so it's not a java project and not a python project.
    it is possible to query the project modules and make some heuristics and decide if it's a java project.
    currently what we test is if the JavaLanguageService is registered, that is good enough to decide if its idea because
    the java plugin is installed only on idea and can not be installed on other IDEs.
    python plugin can be installed on some IDEs and so if the PythonLanguageService is registered that doesn't necessarily
    mean that its pycharm.
     */
    public boolean isJavaProject() {
        return idea.is();
    }

    //this is not accurate, it may be rider but not a C# project
    public boolean isCSharpProject() {
        return rider.is();
    }


    public boolean isIdeaIDE() {
        return idea.is();
    }

    public boolean isRiderIDE() {
        return rider.is();
    }

    public boolean isPyCharmIDE() {
        //python plugin may also be installed on Idea
        return pycharm.is() && !idea.is();
    }


    private static abstract class IsIDE {

        private LanguageService myLanguageService = null;

        private IsIDE(Project project, SupportedLanguages language) {
            init(project, language);
        }

        private void init(Project project, SupportedLanguages language) {
            try {
                @SuppressWarnings("unchecked") Class<LanguageService> languageServiceClass = (Class<LanguageService>) Class.forName(language.getLanguageServiceClassName());
                //noinspection IncorrectServiceRetrieving
                myLanguageService = project.getService(languageServiceClass);
            } catch (Throwable ignored) {
                //catch throwable and not exception because it may be Error like NoClassDefFound
            }
        }

        boolean is() {
            return myLanguageService != null;
        }

        @Nullable
        LanguageService getLanguageService() {
            return myLanguageService;
        }

    }


    private static class Rider extends IsIDE {
        private Rider(Project project) {
            super(project, SupportedLanguages.CSHARP);
        }
    }

    private static class Idea extends IsIDE {
        private Idea(Project project) {
            super(project, SupportedLanguages.JAVA);
        }
    }

    private static class Pycharm extends IsIDE {
        private Pycharm(Project project) {
            super(project, SupportedLanguages.PYTHON);
        }
    }


}
