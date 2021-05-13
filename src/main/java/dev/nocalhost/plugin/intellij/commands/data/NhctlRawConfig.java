package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlRawConfig {
    private String name;
    private String serviceType;
    private ServiceDependLabelSelector dependLabelSelector;
    private List<ServiceContainer> containers;
}