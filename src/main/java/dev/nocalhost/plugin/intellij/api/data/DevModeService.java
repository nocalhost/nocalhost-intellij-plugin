package dev.nocalhost.plugin.intellij.api.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DevModeService {
    private int applicationId;
    private int devSpaceId;
    private String name;

}
