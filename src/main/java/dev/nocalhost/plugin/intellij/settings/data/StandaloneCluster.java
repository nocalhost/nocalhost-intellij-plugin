package dev.nocalhost.plugin.intellij.settings.data;

import com.google.common.base.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StandaloneCluster {
    private String rawKubeConfig;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandaloneCluster that = (StandaloneCluster) o;
        return Objects.equal(rawKubeConfig, that.rawKubeConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rawKubeConfig);
    }
}
