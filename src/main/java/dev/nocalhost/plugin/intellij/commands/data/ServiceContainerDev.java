package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceContainerDev {
    private String gitUrl;
    private String image;
    private String shell;
    private String workDir;
    private Object resources;
    private List<Object> persistentVolumeDirs;
    private ServiceContainerDevCommand command;
    private ServiceContainerDevDebug debug;
    private boolean useDevContainer;
    private Object sync;
    private List<Object> env;
    private List<Object> envFrom;
    private List<String> portForward;
}
