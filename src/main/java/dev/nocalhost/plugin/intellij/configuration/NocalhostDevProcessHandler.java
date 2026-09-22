package dev.nocalhost.plugin.intellij.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.io.BaseOutputReader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;

public class NocalhostDevProcessHandler extends KillableProcessHandler {
    private static final Logger LOG = Logger.getInstance(NocalhostDevProcessHandler.class);

    private final ExecutionEnvironment executionEnvironment;

    public NocalhostDevProcessHandler(
            @NotNull GeneralCommandLine commandLine,
            @NotNull ExecutionEnvironment environment,
            NocalhostProfileState state
    ) throws ExecutionException {
        super(commandLine);
        this.executionEnvironment = environment;
        this.addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(@NotNull ProcessEvent event) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        state.startup();
                    } catch (Exception ex) {
                        ErrorUtil.dealWith(environment.getProject(), "NocalhostProfileState#startup", ex.getMessage(), ex);
                    }
                });
            }

            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                state.destroy();
            }
        });
    }

    @Override
    protected void doDestroyProcess() {
        sendCtrlC();
        super.doDestroyProcess();
    }

    public String getRunProfileName() {
        return this.executionEnvironment.getRunProfile().getName();
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
        // notifyProcessTerminated 是在 ProcessWaitFor 线程上回调的，而 getConsoleView() ->
        // RunContentManager.findContentDescriptor() 内部会更新 Run/Debug 工具窗口的图标，
        // 该操作要求 EDT，否则会抛出 "Assert: must be called on EDT"。
        // 因此这里把输出动作切回 EDT 执行。
        String message = "\n" + NocalhostI18n.format("process.finished", exitCode);
        ApplicationManager.getApplication().invokeLater(() -> print(message, ConsoleViewContentType.SYSTEM_OUTPUT));

        super.notifyProcessTerminated(exitCode);
    }

    private void print(@NotNull String message, @NotNull ConsoleViewContentType consoleViewContentType) {
        ConsoleView console = getConsoleView();
        if (console != null && !Disposer.isDisposed(console)) {
            console.print(message, consoleViewContentType);
        }
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

    @Override
    protected @NotNull BaseOutputReader.Options readerOptions() {
        return new BaseOutputReader.Options() {
            @Override
            public boolean splitToLines() {
                return false;
            }
        };
    }
}
