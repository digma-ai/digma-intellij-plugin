package org.digma.intellij.plugin.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.PlatformUtils;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.SupportedLanguages;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class IDEUtilsService {

    private final IsRider isRider;
    private final IsIdea isIdea;


    public IDEUtilsService(Project project) {
        isRider = new IsRider(project);
        isIdea = new IsIdea(project);
    }

    public static IDEUtilsService getInstance(@NotNull Project project){
        return project.getService(IDEUtilsService.class);
    }


    public static boolean isAlreadyPassedInstallationWizard(){
        PersistenceService persistenceService = PersistenceService.getInstance();
        if (IDEUtilsService.isIdeaIDE() && persistenceService.isAlreadyPassedTheInstallationWizardForIdeaIDE() ||
                IDEUtilsService.isRiderIDE() && persistenceService.isAlreadyPassedTheInstallationWizardForRiderIDE() ||
                IDEUtilsService.isPyCharmIDE() && persistenceService.isAlreadyPassedTheInstallationWizardForPyCharmIDE()
        )   {
            return true;
        }

        return false;
    }


    public static boolean shouldOpenWizard(){
        return !isAlreadyPassedInstallationWizard();
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
        return (PlatformUtils.isIdeaCommunity() || PlatformUtils.isIdeaUltimate()) && isIdea.isIdea();
    }

    public boolean isCSharpProject() {
        return (PlatformUtils.isIdeaCommunity() || PlatformUtils.isIdeaUltimate()) && isRider.isRider();
    }


    public static boolean isIdeaIDE() {
        return PlatformUtils.isIdeaCommunity() || PlatformUtils.isIdeaUltimate();
    }

    public static boolean isRiderIDE() {
        return PlatformUtils.isRider();
    }

    public static boolean isPyCharmIDE() {
        return PlatformUtils.isPyCharm() || PlatformUtils.isPyCharmCommunity() || PlatformUtils.isPyCharmEducational();
    }

    public boolean isRiderAndCSharpFile(@NotNull Project project, VirtualFile file) {

        //it may be a C# file that was opened from vcs, it doesn't count as C# that CSharpLanguageService should handle
        if (file == null) {
            return false;
        }

        if (isRider.isRider()) {
            LanguageService csharpLanguageService = isRider.getCSharpLanguageService();
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                return false;
            }
            return csharpLanguageService.isServiceFor(psiFile.getLanguage());
        }

        return false;
    }

    public boolean isRider() {
        return isRider.isRider();
    }




    private static class IsRider {

        private LanguageService myLanguageService = null;

        public IsRider(Project project) {
            init(project);
        }

        @SuppressWarnings("unchecked")
        private void init(Project project) {
            Class<LanguageService> cshrpLanguageServiceClass;
            try {
                cshrpLanguageServiceClass = (Class<LanguageService>) Class.forName(SupportedLanguages.CSHARP.getLanguageServiceClassName());
                myLanguageService = project.getService(cshrpLanguageServiceClass);
            } catch (Throwable ignored) {
                //catch throwable and not exception because it may be Error like NoClassDefFound
            }
        }

        public boolean isRider() {
            return myLanguageService != null;
        }

        public LanguageService getCSharpLanguageService() {
            return myLanguageService;
        }
    }



    private static class IsIdea {

        private LanguageService myLanguageService = null;

        public IsIdea(Project project) {
            init(project);
        }

        @SuppressWarnings("unchecked")
        private void init(Project project) {
            Class<LanguageService> javaLanguageServiceClass;
            try {
                javaLanguageServiceClass = (Class<LanguageService>) Class.forName(SupportedLanguages.JAVA.getLanguageServiceClassName());
                myLanguageService = project.getService(javaLanguageServiceClass);
            } catch (Throwable ignored) {
                //catch throwable and not exception because it may be Error like NoClassDefFound
            }
        }

        public boolean isIdea() {
            return myLanguageService != null;
        }

        public LanguageService getJavaLanguageService() {
            return myLanguageService;
        }
    }



}
