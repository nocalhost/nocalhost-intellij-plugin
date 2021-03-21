package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevStartOptions extends NhctlGlobalOptions {
    private String deployment;
    private String image;
    private List<String> localSync;
    private String sidecarImage;
    private String storageClass;
    private String syncthingVersion;
    private String workDir;
    private String container;

    public NhctlDevStartOptions(DevSpace devSpace) {
        super(devSpace);
    }
}
