package org.digma.intellij.plugin.editor;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.psi.LanguageService;

@Service(Service.Level.PROJECT)
public final class LatestMethodUnderCaretHolder {


    private LatestMethodInfo latestMethodInfo = null;

    public static LatestMethodUnderCaretHolder getInstance(Project project) {
        return project.getService(LatestMethodUnderCaretHolder.class);
    }

    public LatestMethodInfo getLatestMethodInfo() {
        return latestMethodInfo;
    }

    public void saveLatestMethodUnderCaret(Project project, LanguageService languageService, String methodId) {

        var psiMethod = languageService.getPsiElementForMethod(methodId);
        if (psiMethod != null) {
            latestMethodInfo = new LatestMethodInfo(methodId, psiMethod.getTextRange().getStartOffset(), psiMethod.getTextRange().getEndOffset());
        } else {
            latestMethodInfo = null;
        }
    }

    public void clearLatestMethodInfo() {
        latestMethodInfo = null;
    }


    public static class LatestMethodInfo {

        public final String methodId;
        public final int startOffset;
        public final int endOffset;

        public LatestMethodInfo(String methodId, int startOffset, int endOffset) {

            this.methodId = methodId;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }

}



