package org.digma.intellij.plugin.index;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.io.DataInputOutputUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.SupportedLanguages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

public class DocumentInfoIndex extends SingleEntryFileBasedIndexExtension<DocumentInfo> {

    //todo: rebuild the index on change
    // see PsiManager.getInstance(project).addPsiTreeChangeListener

    public static final ID<Integer, DocumentInfo> DOCUMENT_INFO_INDEX_ID = ID.create("org.digma.intellij.plugin.index.documentInfo");

    public static final Set<String> namesToExclude = new HashSet<>();

    static {
        namesToExclude.add("package-info.java");
        namesToExclude.add("MavenWrapperDownloader.java");
        //add more names that can't be excluded by other rules
    }


    /**
     * a Project reference is needed to get ProjectFileIndex service for filtering files.
     * but the extension class doesn't have a reference to the project and a constructor with Project is
     * not allowed. so the Project reference is captured in the first call to com.intellij.util.indexing.SingleEntryIndexer#computeValue(com.intellij.util.indexing.FileContent)
     * and then used in the InputFilter
     */
    private Project project;

    @Override
    public @NotNull ID<Integer, DocumentInfo> getName() {
        return DOCUMENT_INFO_INDEX_ID;
    }

    @Override
    public FileBasedIndex.@NotNull InputFilter getInputFilter() {

        return new DefaultFileTypeSpecificInputFilter(findSupportedFileTypes()) {

            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {

                if (namesToExclude.contains(file.getName())) {
                    return false;
                }

                //if we already have a reference to the project then filter files with ProjectFileIndex.
                //otherwise filter in only writable files.
                //this is the best effort , see comment on the project field. it may be that we index many unnecessary
                // files, test files , interfaces ,enums ,annotations.
                // java language service will build an empty DocumentInfo for those types so it will not occupy too much
                // disk space.
                // when those types are opened they will be ignored by, see EditorEventsHandler.
                //the way the indexing works is that is will call acceptInput for many files before it calls the
                // computeValue for the first time. and only the first invocation of computeValue we can capture a
                // reference to the project.
                if (DocumentInfoIndex.this.project != null) {
                    ProjectFileIndex.getInstance(project).getModuleForFile(file);
                    boolean isInSourceContent = ProjectFileIndex.getInstance(project).isInSourceContent(file);
                    boolean isInTestSourceContent = ProjectFileIndex.getInstance(project).isInTestSourceContent(file);
                    boolean hasModule = ProjectFileIndex.getInstance(project).getModuleForFile(file) != null;

                    return hasModule && isInSourceContent && !isInTestSourceContent;

                } else {
                    return file.isWritable();
                }
            }
        };
    }


    private FileType[] findSupportedFileTypes() {

        List<FileType> fileTypes = new ArrayList<>();

        //every language service that has a static field called FILE_TYPE of type FileType will be included in the index.
        //can't load the LanguageService objects as intellij services because there is no access to the project here.
        for (SupportedLanguages value : SupportedLanguages.values()) {

            try {
                @SuppressWarnings("unchecked") // the unchecked cast should be ok here
                Class<? extends LanguageService> clazz = (Class<? extends LanguageService>) Class.forName(value.getLanguageServiceClassName());
                FileType fileType = (FileType) clazz.getDeclaredField("FILE_TYPE").get(null);
                fileTypes.add(fileType);

            } catch (Exception e) {
                //ignore: some classes will fail to load , for example the CSharpLanguageService
                //will fail to load if it's not rider because it depends on rider classes.
                //and some will not have FILE_TYPE field
                //don't log, it will happen too many times
            }
        }

        return fileTypes.toArray(new FileType[0]);
    }


    @Override
    public @NotNull SingleEntryIndexer<DocumentInfo> getIndexer() {
        return new SingleEntryIndexer<DocumentInfo>(false) {
            @Override
            protected @Nullable DocumentInfo computeValue(@NotNull FileContent inputData) {

                //There is no easy way to exclude java interfaces,enums and annotations because the file may
                // contain several classes and it must be query with the relevant language service. only the language
                // service can decide if the psi file is an interface or a class or several classes.
                // so java language service will build an empty map for those types.

                PsiFile psiFile = inputData.getPsiFile();
                Project theProject = inputData.getProject();
                //initialize the project when first time in this method
                DocumentInfoIndex.this.project = theProject;
                DocumentInfoIndexBuilder documentInfoIndexBuilder = theProject.getService(DocumentInfoIndexBuilder.class);
                return documentInfoIndexBuilder.build(psiFile);
            }
        };
    }


    @Override
    public @NotNull DataExternalizer<DocumentInfo> getValueExternalizer() {
        return new DataExternalizer<DocumentInfo>() {
            @Override
            public void save(@NotNull DataOutput out, DocumentInfo value) throws IOException {
                IOUtil.writeString(value.getFileUri(), out);

                DataInputOutputUtilRt.writeMap(out, value.getMethods(), key -> IOUtil.writeString(key, out), methodInfo -> {
                    IOUtil.writeString(methodInfo.getId(), out);
                    IOUtil.writeString(methodInfo.getName(), out);
                    IOUtil.writeString(methodInfo.getContainingClass(), out);
                    IOUtil.writeString(methodInfo.getContainingFileUri(), out);
                    IOUtil.writeString(methodInfo.getContainingNamespace(), out);
                    out.writeInt(methodInfo.getOffsetAtFileUri());
                    //todo:write spans
                });
            }

            @Override
            public DocumentInfo read(@NotNull DataInput in) throws IOException {
                String path = IOUtil.readString(in);
                Map<String, MethodInfo> methods = DataInputOutputUtilRt.readMap(in, () -> IOUtil.readString(in), () -> {
                    String id = IOUtil.readString(in);
                    String name = IOUtil.readString(in);
                    String containingClass = IOUtil.readString(in);
                    String containingFileUri = IOUtil.readString(in);
                    String containingNamespace = IOUtil.readString(in);
                    int offsetAtFileUri = in.readInt();
                    //todo: read spans
                    return new MethodInfo(id, name, containingClass, containingNamespace, containingFileUri, offsetAtFileUri, new ArrayList<SpanInfo>());
                });

                return new DocumentInfo(path, methods);
            }


        };
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
