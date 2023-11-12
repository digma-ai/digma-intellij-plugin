package org.digma.intellij.plugin.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.DataInputOutputUtilRt;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.SingleEntryFileBasedIndexExtension;
import com.intellij.util.indexing.SingleEntryIndexer;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Base class for languages that want to index DocumentInfo.
 */
public abstract class DocumentInfoIndex extends SingleEntryFileBasedIndexExtension<DocumentInfo> {

    private static final Logger LOGGER = Logger.getInstance(DocumentInfoIndex.class);

    /**
     * a Project reference is needed to get ProjectFileIndex service for filtering files.
     * but the FileBasedIndexExtension extension class doesn't have a reference to the project and a constructor with Project is
     * not allowed. so the Project reference is captured in the first call to
     * com.intellij.util.indexing.SingleEntryIndexer#computeValue(com.intellij.util.indexing.FileContent)
     * and then used in the InputFilter
     */
    protected Project project;


    protected abstract DocumentInfo buildDocumentInfo(Project theProject, PsiFile psiFile);


    @Override
    public @NotNull SingleEntryIndexer<DocumentInfo> getIndexer() {
        return new SingleEntryIndexer<>(false) {
            @Override
            protected @Nullable DocumentInfo computeValue(@NotNull FileContent inputData) {

                var stopWatch = StopWatch.createStarted();

                try {

                    //There is no easy way to exclude java interfaces,enums and annotations because the file may
                    // contain several classes, and it must be queried with the relevant language service. only the language
                    // service can decide if the psi file is an interface or a class or several classes.
                    // so java language service will build an empty map for those types.

                    PsiFile psiFile = inputData.getPsiFile();
                    Project theProject = inputData.getProject();
                    if (theProject.isDisposed()) {
                        return null;
                    }

                    //capture the project reference when this method is invoked first time
                    DocumentInfoIndex.this.project = theProject;
                    return buildDocumentInfo(theProject, psiFile);
                } catch (Exception e) {

                    //add duration to help us understand why this code throws exception many times. it throws ProcessCanceledException
                    // for read access.
                    //this duration includes all the retries in buildDocumentInfo so consider it.
                    //todo: if we fix it we can remove the duration
                    if (!stopWatch.isStopped()) {
                        stopWatch.stop();
                    }

                    Log.warnWithException(LOGGER, e, "error in indexing {}, took {}", inputData.getPsiFile().getVirtualFile(), stopWatch.getTime(TimeUnit.MILLISECONDS));
                    ErrorReporter.getInstance().reportError("error in indexing", e, Collections.singletonMap("duration", String.valueOf(stopWatch.getTime(TimeUnit.MILLISECONDS))));
                    return null;
                } finally {
                    if (!stopWatch.isStopped()) {
                        stopWatch.stop();
                    }
                }
            }
        };
    }


    @Override
    public @NotNull DataExternalizer<DocumentInfo> getValueExternalizer() {
        return new DataExternalizer<>() {
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
                    return new MethodInfo(id, name, containingClass, containingNamespace, containingFileUri, offsetAtFileUri);
                });

                return new DocumentInfo(path, methods);
            }


        };
    }

    @Override
    public int getVersion() {
        return 2;
    }
}
