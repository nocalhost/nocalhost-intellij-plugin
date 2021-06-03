package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.io.OutputStream;

public class NocalhostLogs extends LogsToolWindowPanel {
    private static final Logger LOG = Logger.getInstance(NocalhostLogs.class);

    private ConsoleView consoleView;
    private ProcessHandler processHandler;

    public NocalhostLogs(Project project) {
        super(false);

        consoleView = new ColoredConsoleView(project);
        add(consoleView.getComponent());

        DefaultActionGroup actionGroup = new DefaultActionGroup(consoleView.createConsoleActions());
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(
                "Nocalhost.Log.Window.Toolbar", actionGroup, false);
        setToolbar(actionToolbar.getComponent());
    }

    public void executeCommand(GeneralCommandLine command) throws ExecutionException {
        processHandler = ProcessHandlerFactory.getInstance().createProcessHandler(command);
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);
    }

    public void terminateProcess() {
        OutputStream outputStream = processHandler.getProcessInput();
        try {
            outputStream.write(3);
            outputStream.flush();
        } catch (IOException e) {
            LOG.warn("Fail to send ctrl+c to remote process", e);
        }
        processHandler.destroyProcess();
    }
}
