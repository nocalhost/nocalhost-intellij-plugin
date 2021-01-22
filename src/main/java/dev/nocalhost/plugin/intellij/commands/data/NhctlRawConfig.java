package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlRawConfig {
    private String name;
    private String gitUrl;
    private List<String> devPorts;
}
