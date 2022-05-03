package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

//todo: delete, happens too many times,opens many files not necessarily related to the project or editor
@Deprecated
public class DocumentManagerFileOpenedListener implements FileDocumentManagerListener{

    private static final Logger LOGGER = Logger.getInstance(DocumentManagerFileOpenedListener.class);

    @Override
    public void fileContentLoaded(@NotNull VirtualFile file, @NotNull Document document) {
        Log.log(LOGGER::info, "DocumentManagerFileOpenedListener.fileContentLoaded: file:{}", file);
    }
}
