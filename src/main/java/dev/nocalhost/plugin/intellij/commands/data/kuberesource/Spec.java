package dev.nocalhost.plugin.intellij.commands.data.kuberesource;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Spec {
    private List<Container> containers;
    private Selector selector;
    private Template template;
    private Template jobTemplate;
}
