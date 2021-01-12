package dev.nocalhost.plugin.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.nocalhost.plugin.intellij.api.data.UserInfo;
import lombok.Getter;
import lombok.Setter;

@State(
        name = "NocalhostSettings",
        storages = {@Storage(value = StoragePathMacros.NON_ROAMABLE_FILE, roamingType = RoamingType.DISABLED)}
)
@Getter
@Setter
public class NocalhostSettings implements PersistentStateComponent<NocalhostSettings> {

    private String baseUrl;
    private String jwt;
    private UserInfo userInfo;

    @Override
    public @Nullable NocalhostSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull NocalhostSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public void clearAuth() {
        baseUrl = null;
        jwt = null;
        userInfo = null;
    }

}
