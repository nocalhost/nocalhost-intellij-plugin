package dev.nocalhost.plugin.intellij.settings;

import com.google.common.base.Objects;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NocalhostRepo implements Serializable {

    private String host;
    private String email;
    private String appName;
    private int devSpaceId;
    private String deploymentName;
    private String repoPath;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NocalhostRepo that = (NocalhostRepo) o;
        return Objects.equal(host, that.host)
                && Objects.equal(email, that.email)
                && Objects.equal(appName, that.appName)
                && Objects.equal(devSpaceId, that.devSpaceId)
                && Objects.equal(deploymentName, that.deploymentName)
                && Objects.equal(repoPath, that.repoPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(host, email, appName, deploymentName, repoPath);
    }
}
