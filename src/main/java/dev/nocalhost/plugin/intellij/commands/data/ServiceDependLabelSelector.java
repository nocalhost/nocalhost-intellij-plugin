package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceDependLabelSelector {
    private List<String> pods;
    private List<String> jobs;
}
