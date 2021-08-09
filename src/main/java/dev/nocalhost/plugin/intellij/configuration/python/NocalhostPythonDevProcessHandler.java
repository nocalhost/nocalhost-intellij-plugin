package dev.nocalhost.plugin.intellij.configuration.python;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.jetbrains.python.debugger.remote.PyRemoteDebugCommandLineState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;

import lombok.SneakyThrows;

public class NocalhostPythonDevProcessHandler extends PyRemoteDebugCommandLineState.PyRemoteDebugProcessHandler {
    private final ExecutionEnvironment environment;

    public NocalhostPythonDevProcessHandler(
        @NotNull ExecutionEnvironment environment,
        @NotNull NocalhostPythonProfileState state
    ) {
        this.environment = environment;
        this.addProcessListener(new ProcessAdapter() {
            @SneakyThrows
            @Override
            public void startNotified(@NotNull ProcessEvent event) {
                state.doStartupDebug();
            }
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                state.doDestroyDebug();
            }
        });
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
                .getInstance(environment.getProject())
                .findContentDescriptor(environment.getExecutor(), this);

        ConsoleView console = null;
        if (contentDescriptor != null && contentDescriptor.getExecutionConsole() instanceof ConsoleView) {
            console = (ConsoleView) contentDescriptor.getExecutionConsole();
        }
        return console;
    }
}
