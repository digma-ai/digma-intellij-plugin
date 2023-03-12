package org.digma.intellij.plugin.persistence;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(
        name = "org.digma.intellij.plugin.persistence.PersistenceService",
        storages = @Storage("DigmaPersistence.xml")
)
public class PersistenceService implements PersistentStateComponent<PersistenceData> {

    private final Project project;

    private final PersistenceData myPersistenceData = new PersistenceData();

    public PersistenceService(Project project) {
        this.project = project;
    }

    @Override
    public @NotNull PersistenceData getState() {
        return myPersistenceData;
    }

    @Override
    public void loadState(@NotNull PersistenceData state) {
        XmlSerializerUtil.copyBean(state, myPersistenceData);
    }
}
