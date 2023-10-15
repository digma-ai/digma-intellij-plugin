package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.impl.source.JavaFileElementType;
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import org.digma.intellij.plugin.index.DocumentInfoIndex;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JavaDocumentInfoIndex extends DocumentInfoIndex {

    public static final ID<Integer, DocumentInfo> JAVA_DOCUMENT_INFO_INDEX_ID =
                                    ID.create("org.digma.intellij.plugin.index.documentInfo.java");

    static final Set<String> namesToExclude = new HashSet<>();

    static {
        namesToExclude.add("package-info.java");
        namesToExclude.add("MavenWrapperDownloader.java");
        //add more names that can't be excluded by other rules
    }

    @Override
    public @NotNull ID<Integer, DocumentInfo> getName() {
        return JAVA_DOCUMENT_INFO_INDEX_ID;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {

        //currently supported file types are java , python is not indexed yet
        return new DefaultFileTypeSpecificInputFilter(JavaFileType.INSTANCE) {

            /**
             * filters file that should be indexed.
             */
            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {

                //filter out some well known files like package-info.java and MavenWrapperDownloader.java
                if (namesToExclude.contains(file.getName())) {
                    return false;
                }

                //if we already have a reference to the project then filter files with ProjectFileIndex.
                //otherwise filter in only writable files and try to use JavaFileElementType.isInSourceContent.
                //this is the best effort , see comment on the project field.
                // it may be that we index many unnecessary files , interfaces ,enums ,annotations.
                // JavaCodeObjectDiscovery will build an empty DocumentInfo for those types, so it will not occupy too much
                // disk space.
                // when those types are opened they will be ignored, see EditorEventsHandler.
                if (JavaDocumentInfoIndex.this.project != null && !JavaDocumentInfoIndex.this.project.isDisposed()) {
                    boolean isInSourceContent = ProjectFileIndex.getInstance(project).isInSourceContent(file);
                    boolean isExcluded = ProjectFileIndex.getInstance(project).isExcluded(file);
                    return isInSourceContent && !isExcluded;
                } else {
                    return file.isWritable() && JavaDocumentInfoIndex.this.isFileInSourceContent(file);
                }
            }
        };
    }


    private boolean isFileInSourceContent(@NotNull VirtualFile file){
        if (JavaFileType.INSTANCE.equals(file.getFileType())) {
            return JavaFileElementType.isInSourceContent(file);
        } else {
            return false;
        }
    }


    @Override
    protected DocumentInfo buildDocumentInfo(Project theProject, PsiFile psiFile) {
        if (psiFile instanceof PsiJavaFile psiJavaFile) {
            return JavaCodeObjectDiscovery.buildDocumentInfoImpl(project, psiJavaFile);
        }
        return null;
    }



    static DocumentInfo tryGetDocumentInfoFromIndex(Project project, @NotNull PsiJavaFile psiFile) {

        DocumentInfo documentInfo = null;
        try {
            Map<Integer, DocumentInfo> documentInfoMap =
                    FileBasedIndex.getInstance().getFileData(JavaDocumentInfoIndex.JAVA_DOCUMENT_INFO_INDEX_ID, psiFile.getVirtualFile(), project);
            //there is only one DocumentInfo per file in the index.
            //all relevant files must be indexed, so if we are here then DocumentInfo must be found if the index is ready,
            // or we have a mistake somewhere else. java interfaces,enums and annotations are indexed but the DocumentInfo
            // object is empty of methods, that's because currently we have no way to exclude those types from indexing.
            documentInfo = documentInfoMap.values().stream().findFirst().orElse(null);

        } catch (IndexNotReadyException e) {
            //ignore
            //IndexNotReadyException will be thrown on dumb mode, when indexing is still in progress.
            //usually it should not happen because this method is called only in smart mode.
        }

        return documentInfo;

    }

}
