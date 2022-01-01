package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlKubeConfigCheckCommand extends BaseCommand {
    private static Process prev;
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
        args.add("-i");
        return fulfill(args);
    }

    @Override
    public String execute() throws IOException, NocalhostExecuteCmdException, InterruptedException {
        destroy();
        return super.execute();
    }

    @Override
    protected void consume(@NotNull Process process) {
        prev = process;
    }

    public static void destroy() {
        if (prev != null) {
            prev.destroy();
        }
    }

    @Getter
    @Setter
    public static class Result {
        private String status;
        private String tips;
    }
}
