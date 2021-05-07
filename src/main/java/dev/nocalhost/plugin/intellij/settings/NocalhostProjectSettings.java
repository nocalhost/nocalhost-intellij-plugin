package dev.nocalhost.plugin.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import lombok.Getter;

@State(name = "NocalhostProjectSettings", storages = @Storage("nocalhost.xml"))
@Getter
public class NocalhostProjectSettings implements PersistentStateComponent<NocalhostProjectSettings> {
    private String serviceProjectPathJson;

    @Override
    public @Nullable NocalhostProjectSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull NocalhostProjectSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public ServiceProjectPath getDevModeService() {
        return DataUtils.GSON.fromJson(serviceProjectPathJson, ServiceProjectPath.class);
    }

    public void setDevModeService(ServiceProjectPath serviceProjectPath) {
        serviceProjectPathJson = DataUtils.GSON.toJson(serviceProjectPath);
    }
}
