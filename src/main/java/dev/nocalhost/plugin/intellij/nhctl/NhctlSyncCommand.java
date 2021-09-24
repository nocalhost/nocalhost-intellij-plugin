package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlSyncCommand extends BaseCommand {
    private boolean resume;
    private String controller;
    private String controllerType;
    private String applicationName;

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "sync", applicationName);
        if (StringUtils.isNotEmpty(controller)) {
            args.add("--controller");
            args.add(controller);
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
