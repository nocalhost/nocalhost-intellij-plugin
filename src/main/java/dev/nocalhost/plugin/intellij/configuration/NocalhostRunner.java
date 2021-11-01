package dev.nocalhost.plugin.intellij.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.openapi.fileEditor.FileDocumentManager;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class NocalhostRunner implements ProgramRunner<RunnerSettings> {
    @Override
    public @NotNull
    @NonNls
    String getRunnerId() {
        return "NocalhostRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof NocalhostConfiguration;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, state -> {
            if (state instanceof NocalhostProfileState) {
                ((NocalhostProfileState) state).prepare();
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