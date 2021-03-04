package dev.nocalhost.plugin.intellij.ui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AppInstallOrUpgradeOption {
    private boolean specifyOneSelected;
    private String specifyText;
}
