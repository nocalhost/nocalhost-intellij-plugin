package dev.nocalhost.plugin.intellij.settings;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.settings.data.StandaloneCluster;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import lombok.Getter;
import lombok.Setter;

@State(
        name = "NocalhostSettings",
        storages = {@Storage(value = StoragePathMacros.NON_ROAMABLE_FILE, roamingType = RoamingType.DISABLED)}
)
@Getter
@Setter
public class NocalhostSettings implements PersistentStateComponent<NocalhostSettings> {

    private String standaloneClustersJson;
    private String nocalhostAccountsJson;

    private Map<String, String> devModeProjectPathServiceMap = Maps.newHashMap();

    private String nhctlBinary;
    private String kubectlBinary;

    public synchronized void updateStandaloneCluster(StandaloneCluster standaloneCluster) {
        Set<StandaloneCluster> set = Sets.newHashSet();
        if (StringUtils.isNotEmpty(standaloneClustersJson)) {
            set = DataUtils.GSON.fromJson(standaloneClustersJson,
                    TypeToken.getParameterized(Set.class, StandaloneCluster.class).getType());
        }
        set.remove(standaloneCluster);
        set.add(standaloneCluster);
        standaloneClustersJson = DataUtils.GSON.toJson(set);
    }

    public synchronized void removeStandaloneCluster(StandaloneCluster standaloneCluster) {
        Set<StandaloneCluster> set = Sets.newHashSet();
        if (StringUtils.isNotEmpty(standaloneClustersJson)) {
            set = DataUtils.GSON.fromJson(standaloneClustersJson,
                    TypeToken.getParameterized(Set.class, StandaloneCluster.class).getType());
        }
        set.remove(standaloneCluster);
        standaloneClustersJson = DataUtils.GSON.toJson(set);
    }

    public synchronized Set<StandaloneCluster> getStandaloneClusters() {
        Set<StandaloneCluster> set = DataUtils.GSON.fromJson(standaloneClustersJson,
                TypeToken.getParameterized(Set.class, StandaloneCluster.class).getType());
        if (set == null) {
            return Sets.newHashSet();
        }
        return set;
    }

    public synchronized void updateNocalhostAccount(NocalhostAccount nocalhostAccount) {
        Set<NocalhostAccount> set = Sets.newHashSet();
        if (StringUtils.isNotEmpty(nocalhostAccountsJson)) {
            set = DataUtils.GSON.fromJson(nocalhostAccountsJson,
                    TypeToken.getParameterized(Set.class, NocalhostAccount.class).getType());
        }
        set.remove(nocalhostAccount);
        set.add(nocalhostAccount);
        nocalhostAccountsJson = DataUtils.GSON.toJson(set);
    }

    public synchronized void removeNocalhostAccount(NocalhostAccount nocalhostAccount) {
        Set<NocalhostAccount> set = Sets.newHashSet();
        if (StringUtils.isNotEmpty(nocalhostAccountsJson)) {
            set = DataUtils.GSON.fromJson(nocalhostAccountsJson,
                    TypeToken.getParameterized(Set.class, NocalhostAccount.class).getType());
        }
        set.remove(nocalhostAccount);
        nocalhostAccountsJson = DataUtils.GSON.toJson(set);
    }

    public synchronized Set<NocalhostAccount> getNocalhostAccounts() {
        Set<NocalhostAccount> set = DataUtils.GSON.fromJson(nocalhostAccountsJson,
                TypeToken.getParameterized(Set.class, NocalhostAccount.class).getType());
        if (set == null) {
            return Sets.newHashSet();
        }
        return set;
    }

    public synchronized ServiceProjectPath getDevModeServiceByProjectPath(String projectPath) {
        String json = devModeProjectPathServiceMap.get(projectPath);
        if (json == null) {
            return null;
        }
        return DataUtils.GSON.fromJson(json, ServiceProjectPath.class);
    }

    public synchronized void setDevModeServiceToProjectPath(ServiceProjectPath serviceProjectPath) {
        devModeProjectPathServiceMap.put(serviceProjectPath.getProjectPath(),
                DataUtils.GSON.toJson(serviceProjectPath));
    }

    public synchronized void removeDevModeServiceByProjectPath(String projectPath) {
        devModeProjectPathServiceMap.remove(projectPath);
    }

    @Override
    public @Nullable NocalhostSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull NocalhostSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}
