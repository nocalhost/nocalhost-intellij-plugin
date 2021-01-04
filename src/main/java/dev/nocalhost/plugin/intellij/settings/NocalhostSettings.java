package dev.nocalhost.plugin.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import dev.nocalhost.plugin.intellij.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "NocalhostSettings", storages = {
        @Storage(value = "nocalhost.xml"),
})
public class NocalhostSettings implements PersistentStateComponent<NocalhostSettings.State> {

    private static final Logger LOG = CommonUtils.LOG;
    private static final String NOCALHOST_SETTINGS_PASSWORD_KEY = "NOCALHOST_SETTINGS_PASSWORD_KEY";

    private State myState = new State();

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }


    static class State {
        @Nullable
        public String HOST = null;
        @Nullable
        public String EMAIL = null;
        @Nullable
        public String TOKEN = null;
    }
}
