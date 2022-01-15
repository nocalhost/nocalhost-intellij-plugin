package dev.nocalhost.plugin.intellij.task;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlRawConfig;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatus;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatusOptions;
import dev.nocalhost.plugin.intellij.configuration.go.NocalhostGoConfigurationType;
import dev.nocalhost.plugin.intellij.configuration.java.NocalhostJavaConfigurationType;
import dev.nocalhost.plugin.intellij.configuration.node.NocalhostNodeConfigurationType;
import dev.nocalhost.plugin.intellij.configuration.php.NocalhostPhpConfigurationType;
import dev.nocalhost.plugin.intellij.configuration.python.NocalhostPythonConfiguration;
import dev.nocalhost.plugin.intellij.configuration.python.NocalhostPythonConfigurationType;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.nhctl.NhctlDevAssociateCommand;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.settings.data.DevModeService;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class ExecutionTask extends Task.Backgroundable {
    public final static String kRun = "run";
    public final static String kDebug = "debug";

    private final String action;
    private final Project project;
    private final DevModeService service;
    private final Map<String, Class<? extends ConfigurationType>> code2conf = new HashMap<>() {
        {
            put("GO", NocalhostGoConfigurationType.class);
            put("PS", NocalhostPhpConfigurationType.class);
            put("WS", NocalhostNodeConfigurationType.class);
            put("IC", NocalhostJavaConfigurationType.class);
            put("IU", NocalhostJavaConfigurationType.class);
            put("PY", NocalhostPythonConfigurationType.class);
        }
    };

    private final Map<String, Class<? extends ConfigurationType>> lang2conf = new HashMap<>() {
        {
            put("go", NocalhostGoConfigurationType.class);
            put("php", NocalhostPhpConfigurationType.class);
            put("node", NocalhostNodeConfigurationType.class);
            put("java", NocalhostJavaConfigurationType.class);
            put("python", NocalhostPythonConfigurationType.class);
        }
    };

    public ExecutionTask(Project project, DevModeService service, String action) {
        super(project, String.format("Starting `%s`", action), true);
        this.action = action;
        this.project = project;
        this.service = service;
    }

    @Override
    public void onThrowable(@NotNull Throwable ex) {
        ErrorUtil.dealWith(this.getProject(), "Failed to start " + action,
                String.format("Error occurred while starting `%s`", action), ex);
    }

    @Override
    @SneakyThrows
    public void run(@NotNull ProgressIndicator indicator) {
        // switch context
        associate(project.getBasePath());
        NocalhostContextManager.getInstance(project).refresh();
        // check sync status
        var path = KubeConfigUtil.toPath(service.getRawKubeConfig());
        var opts = new NhctlSyncStatusOptions(path, service.getNamespace());
        opts.setWait(true);
        opts.setTimeout(600);
        opts.setDeployment(service.getServiceName());
        opts.setControllerType(service.getServiceType());
        var cmd = project.getService(OutputCapturedNhctlCommand.class);
        var text = cmd.syncStatus(service.getApplicationName(), opts);
        var json = DataUtils.GSON.fromJson(text, NhctlSyncStatus.class);
        if ("idle".equals(json.getStatus())) {
            ApplicationManager.getApplication().invokeLater(this::doRun);
        }
    }

    private Executor getExecutor() {
        if (kDebug.equals(action)) {
            return DefaultDebugExecutor.getDebugExecutorInstance();
        }
        return DefaultRunExecutor.getRunExecutorInstance();
    }

    @SneakyThrows
    protected void doRun() {
        // kill active session
        var processes = ExecutionManager.getInstance(project).getRunningProcesses();
        if (processes.length > 0) {
            Arrays.stream(processes).forEach(x -> x.destroyProcess());
            Thread.sleep(1000);
        }

        // start new session
        var builder = ExecutionEnvironmentBuilder.createOrNull(getExecutor(), getConf());
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build());
        }
    }

    @SneakyThrows
    private @NotNull RunnerAndConfigurationSettings getConf() {
        RunnerAndConfigurationSettings conf;
        var manager = RunManager.getInstance(project);
        var type = getConfType();
        var list = manager.getConfigurationSettingsList(type);
        if (list.isEmpty()) {
            conf = manager.createConfiguration("Nocalhost", type);
            manager.addConfiguration(conf);
        } else {
            conf = list.get(0);
        }
        // Python
        if (conf.getType() instanceof NocalhostPythonConfigurationType) {
            var port = getDebugPort();
            if (StringUtils.isEmpty(port)) {
                throw new ExecutionException("The remote debug port is not configured.");
            }
            var pyconf = (NocalhostPythonConfiguration) conf.getConfiguration();
            pyconf.setPort(port);
            pyconf.setHost("127.0.0.1");
        }
        manager.setSelectedConfiguration(conf);
        return conf;
    }

    private @Nullable NhctlRawConfig getDevConfig() throws IOException, NocalhostExecuteCmdException, InterruptedException {
        var path = KubeConfigUtil.toPath(service.getRawKubeConfig());
        var opts = new NhctlConfigOptions(path, service.getNamespace());
        opts.setDeployment(service.getServiceName());
        opts.setControllerType(service.getServiceType());
        return ApplicationManager
                .getApplication()
                .getService(NhctlCommand.class)
                .getConfig(service.getApplicationName(), opts, NhctlRawConfig.class);
    }

    private @Nullable Class<? extends ConfigurationType> getConfType() throws IOException, NocalhostExecuteCmdException, InterruptedException, ExecutionException {
        var config = getDevConfig();
        if (config != null && config.getContainers().size() > 0) {
            String lang = "";
            try {
                lang = config.getContainers().get(0).getDev().getDebug().getLanguage().toLowerCase();
            } catch (Exception ex) {
                // ignore
            }
            if (StringUtils.isNotEmpty(lang)) {
                var conf = lang2conf.getOrDefault(lang, null);
                if (conf == null) {
                    throw new ExecutionException(String.format("Failed to create configuration, language: %s.", lang));
                }
                return conf;
            }
        }
        var code = ApplicationInfo.getInstance().getBuild().getProductCode();
        var conf = code2conf.getOrDefault(code, null);
        if (conf == null) {
            throw new ExecutionException(String.format("Failed to create configuration, IDE: %s.", code));
        }
        return conf;
    }

    private @Nullable String getDebugPort() throws ExecutionException, InterruptedException, NocalhostExecuteCmdException, IOException {
        var config = getDevConfig();
        if (config != null && config.getContainers().size() > 0) {
            try {
                return config.getContainers().get(0).getDev().getDebug().getRemoteDebugPort();
            } catch (Exception ex) {
                // ignore
            }
        }
        throw new ExecutionException("Cannot resolve remoteDebugPort.");
    }

    private void associate(String path) throws IOException, NocalhostExecuteCmdException, InterruptedException {
        var cmd = new NhctlDevAssociateCommand(project);
        cmd.setKubeConfig(KubeConfigUtil.toPath(service.getRawKubeConfig()));
        cmd.setNamespace(service.getNamespace());
        cmd.setLocalSync(Paths.get(path).toString());
        cmd.setContainer(service.getContainerName());
        cmd.setDeployment(service.getServiceName());
        cmd.setControllerType(service.getServiceType());
        cmd.setApplicationName(service.getApplicationName());
        cmd.execute();
    }
}
