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
    private List<String> contexts;

    public NhctlKubeConfigCheckCommand(Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "kubeconfig", "check");
        if (contexts != null) {
            contexts.forEach(x -> {
                args.add("--context");
                args.add(x);
            });
        }
        return fulfill(args);
    }
}
