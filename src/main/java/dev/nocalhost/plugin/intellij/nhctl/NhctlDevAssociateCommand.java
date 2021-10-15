package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevAssociateCommand extends BaseCommand{
    private boolean deAssociate;
    private String localSync;
    private String container;
    private String controllerType;
    private String applicationName;

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "dev", "associate", applicationName);
        if (StringUtils.isNotEmpty(localSync)) {
            args.add("--local-sync");
            args.add(localSync);
        }
        if (StringUtils.isNotEmpty(container)) {
            args.add("--container");
            args.add(container);
        }
        if (StringUtils.isNotEmpty(controllerType)) {
            args.add("--controller-type");
            args.add(controllerType);
        }
        if (deAssociate) {
            args.add("--de-associate");
        }
        return fulfill(args);
    }
}
