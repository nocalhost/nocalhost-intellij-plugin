package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlSyncStatusCommand extends BaseCommand {
    private boolean override;
    private String controllerType;
    private String applicationName;

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "sync-status", applicationName);
        if (StringUtils.isNotEmpty(controllerType)) {
            args.add("--controller-type");
            args.add(controllerType);
        }
        if (override) {
            args.add("--override");
        }
        return fulfill(args);
    }
}
