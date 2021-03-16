package dev.nocalhost.plugin.intellij.commands;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import java.io.File;
import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.exception.NocalhostGitException;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitLineHandlerListener;
import git4idea.commands.GitStandardProgressAnalyzer;

public class GitCommand {
    public void clone(Path parentDir, String url, String clonedDirectoryName, Project project) throws NocalhostGitException {
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        indicator.setIndeterminate(false);
        GitLineHandlerListener progressListener = GitStandardProgressAnalyzer.createListener(indicator);

        NocalhostOutputAppendNotifier publisher = project.getMessageBus()
                                                         .syncPublisher(NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC);

        final CloneOutputResultListener cloneOutputResultListener = new CloneOutputResultListener(publisher);

        GitCommandResult gitCommandResult = Git.getInstance().clone(project, parentDir.toFile(), url, clonedDirectoryName, progressListener, cloneOutputResultListener);
        int exitCode = gitCommandResult.getExitCode();
        if (exitCode != 0) {
            throw new NocalhostGitException(exitCode, gitCommandResult.getErrorOutputAsJoinedString());
        }
    }

    public String getRemote(String path, Project project) {
        GitLineHandler h = new GitLineHandler(project, new File(path), git4idea.commands.GitCommand.REMOTE);
        h.addParameters("-v");
        GitCommandResult gitCommandResult = Git.getInstance().runCommand(h);
        return gitCommandResult.getOutputAsJoinedString();
    }

    private static class CloneOutputResultListener implements GitLineHandlerListener {

        private NocalhostOutputAppendNotifier publisher;

        public CloneOutputResultListener(NocalhostOutputAppendNotifier publisher) {
            this.publisher = publisher;
        }

        @Override
        public void onLineAvailable(String line, Key outputType) {
            if (outputType == ProcessOutputTypes.STDOUT) {
                publisher.action(line + System.lineSeparator());
            }
        }
    }
}
