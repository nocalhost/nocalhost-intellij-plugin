package dev.nocalhost.plugin.intellij.data.nocalhostconfig;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Application {
    private String name;
    private String manifestType;
    private String helmVersion;
    private List<String> resourcePath;
}
