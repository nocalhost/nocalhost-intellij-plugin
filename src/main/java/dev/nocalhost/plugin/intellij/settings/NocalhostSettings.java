package dev.nocalhost.plugin.intellij.settings;

import com.google.common.base.Strings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.nocalhost.plugin.intellij.api.data.UserInfo;

@State(name = "NocalhostSettings", storages = {
        @Storage(value = "nocalhost.xml"),
})
public class NocalhostSettings implements PersistentStateComponent<NocalhostSettings.State> {

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

    public String getHost() {
        return myState.HOST;
    }

    public void setHost(@NotNull String host) {
        myState.HOST = Strings.nullToEmpty(host);
    }

    public String getEmail() {
        return myState.EMAIL;
    }

    public void setEmail(@NotNull String email) {
        myState.EMAIL = Strings.nullToEmpty(email);
    }

    public String getPassword() {
        return myState.PASSWORD;
    }

    public void setPassword(@NotNull String password) {
        myState.PASSWORD = Strings.nullToEmpty(password);
    }

    public String getToken() {
        return myState.TOKEN;
    }

    public void setToken(@NotNull String token) {
        myState.TOKEN = Strings.nullToEmpty(token);
    }

    public UserInfo getUser() {
        return myState.USER;
    }

    public void setUser(UserInfo userInfo) {
        myState.USER = userInfo;
    }

    static class State {
        @Nullable
        public String HOST = null;
        @Nullable
        public String EMAIL = null;
        @Nullable
        public String TOKEN = null;
        @Nullable
        public String PASSWORD = null;
        @Nullable
        public UserInfo USER = null;
    }

    public void clearAuth() {
        myState.HOST = null;
        myState.EMAIL = null;
        myState.TOKEN = null;
        myState.PASSWORD = null;
        myState.USER = null;
    }

}
