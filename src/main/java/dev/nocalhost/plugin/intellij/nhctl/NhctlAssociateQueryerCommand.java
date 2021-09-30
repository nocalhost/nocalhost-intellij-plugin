package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlAssociateQueryerCommand extends BaseCommand {
    private boolean current;
    private String localSync;

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "dev", "associate-queryer");
        if (StringUtils.isNotEmpty(localSync)) {
            args.add("--local-sync");
            args.add(localSync);
        }
        if (current) {
            args.add("--current");
        }
        args.add("--json");
        return fulfill(args);
    }
}
