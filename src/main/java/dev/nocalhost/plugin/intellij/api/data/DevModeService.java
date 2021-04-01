package dev.nocalhost.plugin.intellij.api.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DevModeService {
    private String applicationName;
    private int devSpaceId;
    private String serviceName;
    private String containerName;
}
