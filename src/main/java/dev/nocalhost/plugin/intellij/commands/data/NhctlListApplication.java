package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlListApplication {
    private String namespace;
    private Application[] application;

    @Getter
    @Setter
    public static class Application {
        private String name;
        private String type;
    }
}
