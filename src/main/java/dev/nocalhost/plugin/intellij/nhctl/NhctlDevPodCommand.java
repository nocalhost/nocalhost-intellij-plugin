package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevPodCommand extends BaseCommand{
    private String application;

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "dev", "pod", application);
        return fulfill(args);
    }

    @Override
    public String execute() throws IOException, NocalhostExecuteCmdException, InterruptedException {
        System.lineSeparator()
        return super.execute().replace("\n", "");
    }
}
