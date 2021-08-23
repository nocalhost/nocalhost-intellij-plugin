package dev.nocalhost.plugin.intellij.service;

import com.google.common.collect.MapMaker;

import com.intellij.openapi.progress.Task;

import org.apache.commons.compress.utils.Lists;

import java.util.List;
import java.util.Map;

public class ProgressProcessManager {
    private final Map<Task, List<Process>> map = new MapMaker().makeMap();

    public synchronized void add(Task task, Process process) {
        map.computeIfAbsent(task, key -> Lists.newArrayList());
        map.get(task).add(process);
    }

    public void del(Task task) {
        map.remove(task);
    }

    public List<Process> get(Task task) {
        return map.get(task);
    }
}
