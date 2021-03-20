package dev.nocalhost.plugin.intellij.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;

import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardEndOptions;

public class NocalhostDevProcessHandler extends KillableColoredProcessHandler {
    private static final Logger LOG = Logger.getInstance(NocalhostDevProcessHandler.class);

    private final ExecutionEnvironment executionEnvironment;
    private final NocalhostDevInfo nocalhostDevInfo;

    public NocalhostDevProcessHandler(@NotNull GeneralCommandLine commandLine, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        this(commandLine, environment, null);
    }

    public NocalhostDevProcessHandler(@NotNull GeneralCommandLine commandLine, @NotNull ExecutionEnvironment environment, NocalhostDevInfo nocalhostDevInfo) throws ExecutionException {
        super(commandLine);
        this.executionEnvironment = environment;
        this.nocalhostDevInfo = nocalhostDevInfo;
        this.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                stopDebugPortForward();
            }
        });
    }

    @Override
    protected void notifyProcessTerminated(int exitCode) {
        print(MessageFormat.format("\\nProcess finished with exit code {0}.", exitCode), ConsoleViewContentType.SYSTEM_OUTPUT);

        super.notifyProcessTerminated(exitCode);
    }

    private void print(@NotNull String message, @NotNull ConsoleViewContentType consoleViewContentType) {
        ConsoleView console = getConsoleView();
        if (console != null) console.print(message, consoleViewContentType);
    }

    @Nullable
    private ConsoleView getConsoleView() {
        RunContentDescriptor contentDescriptor = RunContentManager.getInstance(executionEnvironment.getProject())
                .findContentDescriptor(executionEnvironment.getExecutor(), this);

        ConsoleView console = null;
        if (contentDescriptor != null && contentDescriptor.getExecutionConsole() instanceof ConsoleView) {
            console = (ConsoleView) contentDescriptor.getExecutionConsole();
        }
        return console;
    }

    private void stopDebugPortForward() {
        if (nocalhostDevInfo == null) {
            return;
        }

        NocalhostDevInfo.Debug debug = nocalhostDevInfo.getDebug();
        if (debug == null) {
            return;
        }

        DevSpace devSpace = nocalhostDevInfo.getDevSpace();
        Application app = nocalhostDevInfo.getApplication();
        DevModeService devModeService = nocalhostDevInfo.getDevModeService();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                NhctlPortForwardEndOptions nhctlPortForwardEndOptions = new NhctlPortForwardEndOptions(devSpace);
                nhctlPortForwardEndOptions.setPort(debug.getLocalPort() + ":" + debug.getRemotePort());
                nhctlPortForwardEndOptions.setDeployment(devModeService.getServiceName());

                nhctlCommand.endPortForward(app.getContext().getApplicationName(), nhctlPortForwardEndOptions);
            } catch (Exception e) {
                LOG.error(e);
            }
        });
    }
}
