package org.digma.intellij.plugin.psi;

import com.intellij.openapi.util.Getter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.*;
import org.digma.intellij.plugin.common.PsiAccessUtilsKt;
import org.jetbrains.annotations.*;

public class PsiFileCachedValueWithUri implements CachedValue<PsiFile> {

    private final CachedValue<PsiFile> psiFileCachedValue;

    private final String uri;

    public PsiFileCachedValueWithUri(CachedValue<PsiFile> psiFileCachedValue, VirtualFile virtualFile) {
        this.psiFileCachedValue = psiFileCachedValue;
        var psiFile = psiFileCachedValue.getValue();
        this.uri = psiFile == null ? virtualFile.getUrl() : PsiUtils.psiFileToUri(psiFile);
    }

    public String getUri() {
        return uri;
    }

    @Nullable
    @Override
    public PsiFile getValue() {
        return PsiAccessUtilsKt.runInReadAccessWithResult(psiFileCachedValue::getValue);
    }

    @Override
    public @NotNull CachedValueProvider<PsiFile> getValueProvider() {
        return psiFileCachedValue.getValueProvider();
    }

    @Override
    public boolean hasUpToDateValue() {
        return PsiAccessUtilsKt.runInReadAccessWithResult(psiFileCachedValue::hasUpToDateValue);
    }

    @Override
    public Getter<PsiFile> getUpToDateOrNull() {
        return PsiAccessUtilsKt.runInReadAccessWithResult(psiFileCachedValue::getUpToDateOrNull);
    }
}
