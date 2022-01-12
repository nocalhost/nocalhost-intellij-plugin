package dev.nocalhost.plugin.intellij.nhctl;

import com.intellij.openapi.project.Project;
import com.google.common.collect.Lists;
import java.util.List;

public class NhctlCrdListCommand extends BaseCommand {
    public NhctlCrdListCommand(Project project) {
        super(project, false);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "get", "crd-list");
        args.add("--outputType");
        args.add("json");
        return fulfill(args);
    }
}
