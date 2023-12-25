package org.digma.intellij.plugin.common;

import org.digma.intellij.plugin.model.discovery.TextRange;
import org.jetbrains.annotations.Nullable;

public class TextRangeUtils {

    public static TextRange fromJBTextRange(@Nullable com.intellij.openapi.util.TextRange tr) {
        if (tr == null) {
            return new TextRange(0, 0);
        }
        return new TextRange(tr.getStartOffset(), tr.getEndOffset());
    }

}
