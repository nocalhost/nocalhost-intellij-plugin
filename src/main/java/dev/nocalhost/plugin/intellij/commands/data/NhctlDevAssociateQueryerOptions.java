package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDevAssociateQueryerOptions extends NhctlGlobalOptions {
    private String associate;
    private boolean current;
    private List<String> excludeStatus;

    public NhctlDevAssociateQueryerOptions() {
        super();
    }
}
