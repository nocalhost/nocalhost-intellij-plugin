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
    private String associate;

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "dev", "associate-queryer");
        if (StringUtils.isNotEmpty(associate)) {
            args.add("--associate");
            args.add(associate);
        }
        if (current) {
            args.add("--current");
        }
        args.add("--json");
        return args;
    }
}
