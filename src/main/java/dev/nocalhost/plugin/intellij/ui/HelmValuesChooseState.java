package dev.nocalhost.plugin.intellij.ui;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HelmValuesChooseState {
    private boolean specifyValuesYamlSelected;
    private String valuesYamlPath;
    private boolean specifyValues;
    private Map<String,String> values;
}
