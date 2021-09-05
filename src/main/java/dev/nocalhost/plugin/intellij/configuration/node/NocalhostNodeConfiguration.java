package dev.nocalhost.plugin.intellij.configuration.node;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.javascript.debugger.LocalFileSystemFileFinder;
import com.intellij.javascript.debugger.RemoteDebuggingFileFinder;
import com.intellij.javascript.debugger.RemoteDebuggingFileFinderKt;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import com.intellij.xdebugger.XDebugSessionListener;
import com.jetbrains.debugger.wip.BrowserChromeDebugProcess;
import com.jetbrains.debugger.wip.JSRemoteDebugConfiguration;

import dev.nocalhost.plugin.intellij.configuration.NocalhostConfiguration;
import dev.nocalhost.plugin.intellij.configuration.NocalhostProfileState;
import dev.nocalhost.plugin.intellij.configuration.NocalhostSettingsEditor;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import com.intellij.xdebugger.XDebugProcess;
import com.intellij.execution.ExecutionResult;
import com.intellij.xdebugger.XDebugSession;
import java.net.InetSocketAddress;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.options.SettingsEditor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.debugger.DebuggableRunConfiguration;
import com.intellij.javascript.JSRunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.jetbrains.debugger.wip.WipWithExclusiveWebsocketChannelVmConnection;

public class NocalhostNodeConfiguration
        extends LocatableConfigurationBase<NocalhostNodeConfiguration>
        implements RunConfigurationWithSuppressedDefaultRunAction, JSRunProfileWithCompileBeforeLaunchOption, DebuggableRunConfiguration, NocalhostConfiguration
{
    private final JSRemoteDebugConfiguration conf;
    private static final long POLL_TIMEOUT = 10 * 60 * 1000;

    public int getPort() {
        return conf.getPort();
    }

    public void setPort(final int value) {
        conf.setPort(value);
    }

    @NotNull
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new NocalhostSettingsEditor();
    }

    @NotNull
    public RunProfileState getState(@NotNull final Executor executor, @NotNull final ExecutionEnvironment env) {
        return new NocalhostProfileState(env);
    }

    @NotNull
    public InetSocketAddress computeDebugAddress(@NotNull final RunProfileState state) {
        return conf.computeDebugAddress(state);
    }

    @NotNull
    public XDebugProcess createDebugProcess(@NotNull final InetSocketAddress socketAddress, @NotNull final XDebugSession session, @Nullable final ExecutionResult executionResult, @NotNull final ExecutionEnvironment environment) {
        final WipWithExclusiveWebsocketChannelVmConnection connection = new WipWithExclusiveWebsocketChannelVmConnection();
        final RemoteDebuggingFileFinder finder = new RemoteDebuggingFileFinder(RemoteDebuggingFileFinderKt.createUrlToLocalMap(conf.getMappings()), new LocalFileSystemFileFinder());
        final BrowserChromeDebugProcess process = new BrowserChromeDebugProcess(session, finder, connection, executionResult);
        var future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // https://github.com/nodejs/node/issues/9617
            final String url = "http://127.0.0.1:" + conf.getPort() + "/json";
            final Request request = new Request.Builder().url(url).get().build();
            final OkHttpClient client = new OkHttpClient();
            final long timestamp = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() - timestamp > POLL_TIMEOUT) {
                    NocalhostNotifier
                            .getInstance(environment.getProject())
                            .notifyError("NocalhostNodeConfiguration", "The attempt to connect to the remote debug port timed out.");
                    session.stop();
                    break;
                }
                try {
                    Thread.sleep(500);
                    var response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        break;
                    }
                } catch (Exception ex) {
                    // ignored
                }
            }
            connection.open(socketAddress);
        });
        session.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionStopped() {
                future.cancel(true);
            }
        });
        return process;
    }

    public NocalhostNodeConfiguration(@NotNull final Project project, @NotNull final ConfigurationFactory factory, @NotNull final String name) {
        super(project, factory, name);
        this.conf = new JSRemoteDebugConfiguration(project, factory, name);
        this.conf.setHost("127.0.0.1");
    }
}

