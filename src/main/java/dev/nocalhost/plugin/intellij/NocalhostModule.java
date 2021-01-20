package dev.nocalhost.plugin.intellij;

import com.google.common.base.Suppliers;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.intellij.openapi.diagnostic.Logger;

import java.util.function.Supplier;

import dev.nocalhost.plugin.intellij.commands.NocalhostCommandModule;
import dev.nocalhost.plugin.intellij.ui.NocalhostWindow;
import dev.nocalhost.plugin.intellij.ui.action.NocalhostActionModule;

public class NocalhostModule extends AbstractModule {

    public static final Logger LOG = Logger.getInstance("Nocalhost");

    protected static final Supplier<Injector> injector = Suppliers.memoize(() -> Guice.createInjector(new NocalhostModule()));

    public static <T> T getInstance(Class<T> type) {
        return injector.get().getInstance(type);
    }

    @Override
    protected void configure() {
        bind(Logger.class).toInstance(LOG);
        bind(NocalhostWindow.class);

        install(new NocalhostActionModule());
        install(new NocalhostCommandModule());
    }
}
