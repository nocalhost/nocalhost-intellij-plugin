package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlProxyCommand extends BaseCommand {
    private String action;
    private String workload;

    public NhctlProxyCommand(Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "vpn", action);
        if (StringUtils.isNotEmpty(workload)) {
            args.add("--workloads");
            args.add(workload);
        }
        return fulfill(args);
    }
}
