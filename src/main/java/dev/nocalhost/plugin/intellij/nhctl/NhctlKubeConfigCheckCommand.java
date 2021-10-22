package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlKubeConfigCheckCommand extends BaseCommand {
    private String context;

    public NhctlKubeConfigCheckCommand(Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "kubeconfig", "check");
        if (StringUtils.isNotEmpty(context)) {
            args.add("--context");
            args.add(context);
        }
        return fulfill(args);
    }
}
