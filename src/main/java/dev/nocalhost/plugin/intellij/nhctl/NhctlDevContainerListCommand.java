package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevContainerListCommand extends BaseCommand{
    private String application;

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "dev", "containers", application);
        return fulfill(args);
    }
}
