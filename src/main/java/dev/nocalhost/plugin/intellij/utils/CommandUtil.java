package dev.nocalhost.plugin.intellij.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public final class CommandUtil {
    public static void addArg(List<String> args, String flag, String value) {
        if (StringUtils.isNotEmpty(value)) {
            args.add(flag);
            args.add(value);
        }
    }

    public static void addArg(List<String> args, String flag, List<String> values) {
        if (values != null) {
            for (String value : values) {
                if (StringUtils.isNotEmpty(value)) {
                    args.add(flag);
                    args.add(value);
                }
            }
        }
    }
}
