package dev.nocalhost.plugin.intellij.configuration;

import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.data.NocalhostContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class NocalhostRunnerContext {
    private Debug debug;
    private String shell;
    private Command command;
    private NocalhostContext context;
    private ServiceContainer container;

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

