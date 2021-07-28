package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;

import java.io.IOException;
import java.io.OutputStream;

public class NocalhostLogs extends LogsToolWindowPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(NocalhostLogs.class);

    private ConsoleView consoleView;
    private ProcessHandler processHandler;

    public NocalhostLogs(Project project) {
        super(false);

        consoleView = new ColoredConsoleView(project);
        Disposer.register(this, consoleView);
        add(consoleView.getComponent());

        DefaultActionGroup actionGroup = new DefaultActionGroup(consoleView.createConsoleActions());
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(
                "Nocalhost.Log.Window.Toolbar", actionGroup, false);
        actionToolbar.setTargetComponent(this);
        setToolbar(actionToolbar.getComponent());
    }

    public void executeCommand(GeneralCommandLine command) throws ExecutionException {
        processHandler = ProcessHandlerFactory.getInstance().createProcessHandler(command);
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);
    }

    public void terminateCommandProcess() {
        if (processHandler.isProcessTerminated()) {
            return;
        }
        OutputStream outputStream = processHandler.getProcessInput();
        try {
            outputStream.write(3);
            outputStream.flush();
        } catch (IOException e) {
            LOG.warn("Fail to send ctrl+c to remote process", e);
        }
        processHandler.destroyProcess();
    }

    @Override
    public void dispose() {

    }
}
