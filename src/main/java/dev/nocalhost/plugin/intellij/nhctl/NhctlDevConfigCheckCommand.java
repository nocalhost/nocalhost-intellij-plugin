package dev.nocalhost.plugin.intellij.nhctl;

import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.Lists;

import com.intellij.openapi.project.Project;

import java.util.List;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class NhctlDevConfigCheckCommand extends BaseCommand {
    private String application;
    private String controllerType;

    public NhctlDevConfigCheckCommand(Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "ide", "config", application);
        if (StringUtils.isNotEmpty(controllerType)) {
            args.add("--controller-type");
            args.add(controllerType);
        }
        args.add("--action");
        args.add("check");
        return fulfill(args);
    }
}
