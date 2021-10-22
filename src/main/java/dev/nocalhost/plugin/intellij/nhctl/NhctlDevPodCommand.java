package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevPodCommand extends BaseCommand{
    private String application;
    private String controllerType;

    public NhctlDevPodCommand(Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "dev", "pod", application);
        if (StringUtils.isNotEmpty(controllerType)) {
            args.add("--controller-type");
            args.add(controllerType);
        }
        return fulfill(args);
    }
}
