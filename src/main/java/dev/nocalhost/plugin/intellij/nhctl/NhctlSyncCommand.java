package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlSyncCommand extends BaseCommand {
    private boolean resume;
    private String container;
    private String controllerType;
    private String applicationName;

    public NhctlSyncCommand(Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "sync", applicationName);
        if (StringUtils.isNotEmpty(container)) {
            args.add("--container");
            args.add(container);
        }
        if (StringUtils.isNotEmpty(controllerType)) {
            args.add("--controller-type");
            args.add(controllerType);
        }
        if (resume) {
            args.add("--resume");
        }
        return fulfill(args);
    }
}
