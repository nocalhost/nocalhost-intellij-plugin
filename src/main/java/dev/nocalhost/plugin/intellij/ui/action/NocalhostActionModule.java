package dev.nocalhost.plugin.intellij.ui.action;

import com.google.inject.AbstractModule;

public class NocalhostActionModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(RefreshAction.class);
        bind(LogoutAction.class);
    }
}
