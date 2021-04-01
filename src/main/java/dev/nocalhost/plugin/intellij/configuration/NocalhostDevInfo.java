package dev.nocalhost.plugin.intellij.configuration;

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class NocalhostDevInfo {
    private Command command;
    private Debug debug;
    private String shell;
    private DevSpace devSpace;
    private String application;
    private DevModeService devModeService;

    @AllArgsConstructor
    @Getter
    @Setter
    public static class Command {
        private String run;
        private String debug;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class Debug {
        private String remotePort;
        private String localPort;
    }
}

