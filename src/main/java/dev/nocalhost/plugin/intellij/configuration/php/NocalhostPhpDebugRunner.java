package dev.nocalhost.plugin.intellij.configuration.php;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import dev.nocalhost.plugin.intellij.configuration.NocalhostProfileState;
import dev.nocalhost.plugin.intellij.ui.SyncStatusPresentation;

public class NocalhostPhpDebugRunner implements ProgramRunner<RunnerSettings> {
    public static final String RUNNER_ID = "NocalhostPhpDebugRunner";
    private static final Logger LOG = Logger.getInstance(SyncStatusPresentation.class);

    @NotNull
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof NocalhostPhpConfiguration;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        final Project project = environment.getProject();
        ExecutionManager.getInstance(project).startRunProfile(environment, state -> {
            if (state instanceof NocalhostProfileState) {
                ((NocalhostProfileState) state).prepareDevInfo();
            }

            FileDocumentManager.getInstance().saveAllDocuments();
            ExecutionResult executionResult = state.execute(environment.getExecutor(), this);
            if (executionResult == null) {
                return null;
            }
            return new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
        });
    }
}
