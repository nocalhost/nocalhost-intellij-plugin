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
import com.intellij.openapi.fileEditor.FileDocumentManager;
import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.configuration.NocalhostProfileState;

public class NocalhostPhpDebugRunner implements ProgramRunner<RunnerSettings> {
    public static final String RUNNER_ID = "NocalhostPhpDebugRunner";

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
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, state -> {
            if (state instanceof NocalhostProfileState) {
                ((NocalhostProfileState) state).prepareDevInfo();
                // TODO: create SSH tunnel
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
