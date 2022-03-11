package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.collect.Lists;

import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlGetCommand extends BaseCommand{
    private String kind;
    private String name;
    private String application;

    public NhctlGetCommand(Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "get", kind);
        if (StringUtils.isNotEmpty(name)) {
            args.add(name);
        }
        if (StringUtils.isNotEmpty(application)) {
            args.add("--application");
            args.add(application);
        }

        args.add("--outputType");
        args.add("json");
        return fulfill(args);
    }
}

