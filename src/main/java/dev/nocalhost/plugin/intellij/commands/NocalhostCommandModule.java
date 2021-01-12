package dev.nocalhost.plugin.intellij.commands;

import com.google.inject.AbstractModule;

public class NocalhostCommandModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(NhctlClient.class);
    }
}
