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
import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

public class NocalhostDevProcessHandler extends KillableColoredProcessHandler {
    private static final Logger LOG = Logger.getInstance(NocalhostDevProcessHandler.class);

    private final ExecutionEnvironment executionEnvironment;
    private final NocalhostProfileState nocalhostProfileState;

    public NocalhostDevProcessHandler(
            @NotNull GeneralCommandLine commandLine,
            @NotNull ExecutionEnvironment environment,
            NocalhostProfileState nocalhostProfileState
    ) throws ExecutionException {
        super(commandLine);
        this.executionEnvironment = environment;
        this.nocalhostProfileState = nocalhostProfileState;
        this.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                NocalhostDevProcessHandler.this.nocalhostProfileState.doRemoveTunnel();
                NocalhostDevProcessHandler.this.nocalhostProfileState.stopDebugPortForward();
            }
        });
    }

    @Override
    protected void doDestroyProcess() {
        sendCtrlC();
        super.doDestroyProcess();
    }

    private void sendCtrlC() {
        OutputStream outputStream = this.getProcess().getOutputStream();
        try {
            outputStream.write(3);
            outputStream.flush();
        } catch (IOException e) {
            LOG.warn("Fail to send ctrl+c to remote process", e);
        }
    }

    @Override
    protected void notifyProcessTerminated(int exitCode) {
        print(MessageFormat.format("\\nProcess finished with exit code {0}.", exitCode),
                ConsoleViewContentType.SYSTEM_OUTPUT);

        super.notifyProcessTerminated(exitCode);
    }

    private void print(@NotNull String message, @NotNull ConsoleViewContentType consoleViewContentType) {
        ConsoleView console = getConsoleView();
        if (console != null) console.print(message, consoleViewContentType);
    }

    @Nullable
    private ConsoleView getConsoleView() {
        RunContentDescriptor contentDescriptor = RunContentManager
                .getInstance(executionEnvironment.getProject())
                .findContentDescriptor(executionEnvironment.getExecutor(), this);

        ConsoleView console = null;
        if (contentDescriptor != null && contentDescriptor.getExecutionConsole() instanceof ConsoleView) {
            console = (ConsoleView) contentDescriptor.getExecutionConsole();
        }
        return console;
    }
}
