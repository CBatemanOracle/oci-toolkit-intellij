package com.oracle.oci.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.APP)
@State(name = "OCIApplicationSettings",
        storages = @Storage(value = "oracleocitoolkit.xml"))
public final class OCIApplicationSettings implements PersistentStateComponent<OCIApplicationSettings.State> {
    private State state = new State();
    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public static class State{
        private boolean appStackIntroductoryStepShow = true;

        public State(){

        }

        public boolean isAppStackIntroductoryStepShow() {
            return appStackIntroductoryStepShow;
        }

        public void setAppStackIntroductoryStepShow(boolean appStackIntroductoryStep) {
            this.appStackIntroductoryStepShow = appStackIntroductoryStep;
        }
    }

    public static OCIApplicationSettings getInstance(@NotNull Project project){

        OCIApplicationSettings settings =      ApplicationManager.getApplication().getService(OCIApplicationSettings.class);

        return settings;
    }
}
