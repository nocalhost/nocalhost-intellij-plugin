package dev.nocalhost.plugin.intellij;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

public class Version {
    private static final String PLUGIN_VERSION = PluginManagerCore.getPlugin(PluginId.getId("dev.nocalhost.nocalhost-intellij-plugin")).getVersion();

    private Version() {}

    public static String get() {
        return PLUGIN_VERSION;
    }
}
