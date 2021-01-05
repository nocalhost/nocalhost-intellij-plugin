package dev.nocalhost.plugin.intellij;

import com.google.common.base.Suppliers;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.nocalhost.plugin.intellij.ui.NocalhostConsoleWindow;
import dev.nocalhost.plugin.intellij.ui.NocalhostWindow;

import java.util.function.Supplier;

public class NocalhostModule extends AbstractModule {
    protected static final Supplier<Injector> injector = Suppliers.memoize(() -> Guice.createInjector(new NocalhostModule()));

    public static <T> T getInstance(Class<T> type) {
        return injector.get().getInstance(type);
    }

    @Override
    protected void configure() {
        bind(NocalhostWindow.class);
        bind(NocalhostConsoleWindow.class);
    }
}
