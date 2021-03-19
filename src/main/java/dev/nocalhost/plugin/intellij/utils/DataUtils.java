package dev.nocalhost.plugin.intellij.utils;

import com.google.gson.Gson;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

public class DataUtils {

    public static final Gson GSON = new Gson();

    public static final Yaml YAML;

    static {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        YAML = new Yaml(representer);
    }
}
