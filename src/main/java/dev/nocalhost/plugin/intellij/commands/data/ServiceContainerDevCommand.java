package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceContainerDevCommand {
    private List<String> build;
    private List<String> run;
    private List<String> debug;
    private List<String> hotReloadRun;
    private List<String> hotReloadDebug;
}
