package dev.nocalhost.plugin.intellij.nhctl;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.List;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlKubeConfigRenderCommand extends BaseCommand {
    private String port;
    private String context;
    private String address;
    private static String EOF = "EOF";
    private static Map<String, String> confMap = Maps.newHashMap();
    private static Map<String, Process> procMap = Maps.newHashMap();
    private static final Logger LOG = Logger.getInstance(NhctlKubeConfigRenderCommand.class);

    public NhctlKubeConfigRenderCommand(@NotNull Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "kubeconfig", "render");
        if (StringUtils.isNotEmpty(address)) {
            args.add(address);
        }
        if (StringUtils.isNotEmpty(port)) {
            args.add(":" + port);
        }
        if (StringUtils.isNotEmpty(context)) {
            args.add("--context");
            args.add(context);
        }
        return fulfill(args);
    }

    public static boolean isAlive(@NotNull String path) {
        var proc = procMap.get(path);
        if (proc != null) {
            return proc.isAlive();
        }
        return false;
    }

    public static void destroy(@NotNull String path) {
        var proc = procMap.get(path);
        if (proc != null) {
            proc.destroy();
        }
    }

    public static @Nullable String getConf(@NotNull String path) {
        return confMap.get(path);
    }

    @Override
    public String execute() throws IOException, NocalhostExecuteCmdException, InterruptedException {
        var commandLine = getCommandline(compute());
        var cmd = commandLine.getCommandLineString();
        print("[cmd] " + cmd);

        try {
            process = commandLine.createProcess();
            procMap.put(kubeConfig.toString(), process);
            ProjectManager.getInstance().addProjectManagerListener(project, new ProjectManagerListener() {
                @Override
                public void projectClosed(@NotNull Project project) {
                    process.destroy();
                }
            });
        } catch (ExecutionException ex) {
            throw new NocalhostExecuteCmdException(cmd, -1, ex.getMessage());
        }

        var stderr = new AtomicReference<>("");
        var stdout = new AtomicReference<>("");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try (var reader = new InputStreamReader(process.getErrorStream(), Charsets.UTF_8)) {
                stderr.set(CharStreams.toString(reader));
            } catch (Exception ex) {
                // ignore
            }
        });

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                process.waitFor();
            } catch (Exception ex) {
                // ignore
            } finally {
                procMap.remove(kubeConfig.toString());
                confMap.remove(kubeConfig.toString());
            }
        });

        var reader = new InputStreamReader(process.getInputStream(), Charsets.UTF_8);
        try (var br = new BufferedReader(reader)) {
            String text;
            while ((text = br.readLine()) != null) {
                print(text);
                stdout.set(stdout.get() + text + "\n");
                if (StringUtils.equals(text, EOF)) {
                    var raw = stdout.toString().replace(EOF + "\n", "");
                    confMap.put(kubeConfig.toString(), raw);
                    return raw;
                }
            }
        }

        throw new NocalhostExecuteCmdException(cmd, -1, stdout.get() + System.lineSeparator() + stderr.get());
    }
}
