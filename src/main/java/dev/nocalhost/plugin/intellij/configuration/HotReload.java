package dev.nocalhost.plugin.intellij.configuration;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatus;
import dev.nocalhost.plugin.intellij.service.NocalhostProjectService;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class HotReload implements Disposable {
    private static final Logger LOG = Logger.getInstance(HotReload.class);

    private Process process;
    private MessageBusConnection connection;
    private ExecutionEnvironment environment;
    private AtomicBoolean hasFileChanged = new AtomicBoolean(false);

    public HotReload(@NotNull ExecutionEnvironment environment) {
        this.environment = environment;
        connection = environment.getProject().getMessageBus().connect();
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            public void after(@NotNull List<? extends VFileEvent> events) {
                var project = environment.getProject();
                for (VFileEvent event: events) {
                    if (isInIdea(event.getPath())) {
                        continue;
                    }
                    if (ProjectFileIndex.getInstance(project).isInContent(event.getFile())) {
                        echo("[event]" + event + System.lineSeparator());
                        hasFileChanged.compareAndSet(false, true);
                    }
                }
            }
        });
    }

    private boolean isInIdea(String path) {
        return Arrays.stream(path.split(File.separator)).anyMatch(x -> x.equals(".idea"));
    }

    public void dispose() {
        if (process != null) {
            var output = process.getOutputStream();
            try {
                output.write(3);
                output.flush();
            } catch (IOException ex) {
                LOG.warn("[hot-reload] Fail to send ctrl+c to remote process", ex);
            } finally {
                process.destroy();
            }
        }
        connection.dispose();
    }

    private void echo(@NotNull String text) {
        environment
                .getProject()
                .getMessageBus()
                .syncPublisher(NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC)
                .action(text + System.lineSeparator());
    }

    public HotReload withExec() throws ExecutionException {
        var project = environment.getProject();
        var service = project.getService(NocalhostProjectService.class).getServiceProjectPath();
        var cmd = new GeneralCommandLine(Lists.newArrayList(
                NhctlUtil.binaryPath(),
                "sync-status",
                service.getApplicationName(),
                "--namespace",
                service.getNamespace(),
                "--deployment",
                service.getServiceName(),
                "--controller-type",
                service.getServiceType(),
                "--kubeconfig",
                service.getKubeConfigPath().toString(),
                "--watch"
        )).withRedirectErrorStream(true);

        echo("[cmd] " + cmd.getCommandLineString());
        process = cmd.createProcess();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var reader = new InputStreamReader(process.getInputStream(), Charsets.UTF_8);
            try (var br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    echo(line);
                    if (hasFileChanged.get()) {
                        var parsed = DataUtils.GSON.fromJson(line, NhctlSyncStatus.class);
                        if ("idle".equals(parsed.getStatus())) {
                            ExecutionManager.getInstance(environment.getProject()).restartRunProfile(environment);
                            break;
                        }
                    }
                }
                var code = process.waitFor();
                if (code != 0) {
                    echo("[hot-reload] Process finished with exit code " + code);
                }
            } catch (Exception ex) {
                LOG.error(ex);
            }
        });
        return this;
    }
}
