package dev.nocalhost.plugin.intellij.api.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DevModeService {
    private int applicationId;
    private int devSpaceId;
    private String name;

}
