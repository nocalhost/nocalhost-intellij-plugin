package dev.nocalhost.plugin.intellij.settings;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.UserInfo;
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

    private String baseUrl;
    private String jwt;
    private UserInfo userInfo;
    private Map<String, DevModeService> devModeProjectBasePath2Service = Maps.newConcurrentMap();
    private String nhctlBinary;
    private String kubectlBinary;
    private String nocalhostRepos;
    private int apiErrorCount;

    @Override
    public @Nullable NocalhostSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull NocalhostSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public void clearAuth() {
        jwt = null;
        userInfo = null;
    }

    public void addRepos(NocalhostRepo nocalhostRepo) {
        Set<NocalhostRepo> nocalhostRepoSet;
        if (StringUtils.isBlank(nocalhostRepos)) {
            nocalhostRepoSet = Sets.newHashSet();
        } else {
            Type setType = new TypeToken<HashSet<NocalhostRepo>>() {
            }.getType();
            nocalhostRepoSet = DataUtils.GSON.fromJson(nocalhostRepos, setType);
        }
        nocalhostRepoSet.add(nocalhostRepo);
        nocalhostRepos = DataUtils.GSON.toJson(nocalhostRepoSet);
    }

    public Set<NocalhostRepo> getRepos() {
        if (StringUtils.isBlank(nocalhostRepos)) {
            return Sets.newHashSet();
        }
        Type setType = new TypeToken<HashSet<NocalhostRepo>>(){
        }.getType();
        return DataUtils.GSON.fromJson(nocalhostRepos, setType);
    }

    public void removeRepos(NocalhostRepo nocalhostRepo) {
        Set<NocalhostRepo> nocalhostRepoSet;
        if (StringUtils.isBlank(nocalhostRepos)) {
            nocalhostRepoSet = Sets.newHashSet();
        } else {
            Type setType = new TypeToken<HashSet<NocalhostRepo>>() {
            }.getType();
            nocalhostRepoSet = DataUtils.GSON.fromJson(nocalhostRepos, setType);
        }
        nocalhostRepoSet.remove(nocalhostRepo);
        nocalhostRepos = DataUtils.GSON.toJson(nocalhostRepoSet);
    }
}
