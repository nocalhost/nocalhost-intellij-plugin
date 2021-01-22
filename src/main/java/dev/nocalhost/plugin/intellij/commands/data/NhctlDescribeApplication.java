package dev.nocalhost.plugin.intellij.commands.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlDescribeApplication {
    private String name;
    private String releasename;
    private String actualName;
}
