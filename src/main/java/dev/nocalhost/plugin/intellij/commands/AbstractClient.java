package dev.nocalhost.plugin.intellij.commands;

import java.util.List;

public abstract class AbstractClient {

    protected ProcessBuilder createProcessBuilder(List<String> cmd) {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.environment().clear();
        return processBuilder;
    }

    protected ProcessBuilder createProcessBuilder(String... cmd) {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.environment().clear();
        return processBuilder;
    }
}
