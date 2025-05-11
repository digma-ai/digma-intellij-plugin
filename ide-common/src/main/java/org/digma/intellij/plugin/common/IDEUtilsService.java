package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

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
        return isIdeaIDE();
    }

    public boolean isIdeaIDE() {
        return ApplicationInfo.getInstance().getBuild().getProductCode().startsWith("IU") ||
                ApplicationInfo.getInstance().getBuild().getProductCode().startsWith("IC");
    }

}
