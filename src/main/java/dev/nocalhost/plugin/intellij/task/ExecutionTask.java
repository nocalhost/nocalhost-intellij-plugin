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
import dev.nocalhost.plugin.intellij.configuration.php.NocalhostPhpConfigurationType;
import dev.nocalhost.plugin.intellij.configuration.python.NocalhostPythonConfiguration;
import dev.nocalhost.plugin.intellij.configuration.python.NocalhostPythonConfigurationType;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
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
    private final Map<String, Class<? extends ConfigurationType>> hash = new HashMap<>() {
        {
            put("GO", NocalhostGoConfigurationType.class);
            put("PS", NocalhostPhpConfigurationType.class);
            put("IU", NocalhostJavaConfigurationType.class);
            put("PY", NocalhostPythonConfigurationType.class);
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
        ErrorUtil.dealWith(this.getProject(), "Nocalhost",
                String.format("Error occurred while starting `%s`", action), ex);
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        var path = KubeConfigUtil.kubeConfigPath(service.getRawKubeConfig());
        var opts = new NhctlSyncStatusOptions(path, service.getNamespace());
        opts.setWait(true);
        opts.setDeployment(service.getServiceName());
        opts.setControllerType(service.getServiceType());
        var cmd = project.getService(OutputCapturedNhctlCommand.class);
        var text = cmd.syncStatus(service.getApplicationName(), opts);
        var json = DataUtils.GSON.fromJson(text, NhctlSyncStatus.class);
        if ("idle".equals(json.getStatus())) {
            doRun();
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
        if (type == null) {
            throw new ExecutionException("Cannot find the corresponding `ConfigurationType`.");
        }
        var list = manager.getConfigurationSettingsList(type);
        if (list.isEmpty()) {
            conf = manager.createConfiguration("Nocalhost", type);
            manager.addConfiguration(conf);
        } else {
            conf = list.get(0);
        }
        // Python
        if (conf.getType() instanceof NocalhostPythonConfigurationType) {
            var port = getDebugPort(service);
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

    private @Nullable Class<? extends ConfigurationType> getConfType() {
        var ide = ApplicationInfo.getInstance().getBuild().getProductCode();
        return hash.containsKey(ide) ? hash.get(ide) : null;
    }

    private @Nullable String getDebugPort(@NotNull DevModeService service) throws ExecutionException, InterruptedException, NocalhostExecuteCmdException, IOException {
        var cmd = ApplicationManager.getApplication().getService(NhctlCommand.class);
        var path = KubeConfigUtil.kubeConfigPath(service.getRawKubeConfig());
        var opts = new NhctlConfigOptions(path, service.getNamespace());
        opts.setDeployment(service.getServiceName());
        opts.setControllerType(service.getServiceType());
        var config = cmd.getConfig(service.getApplicationName(), opts, NhctlRawConfig.class);
        var bucket = config.getContainers();
        var container = bucket.isEmpty() ? null : bucket.get(0);
        try {
            return container.getDev().getDebug().getRemoteDebugPort();
        } catch (Exception ex) {
            throw new ExecutionException("The configuration of the service container is incorrect.");
        }
    }

    public static @NotNull String asKey(@NotNull String path) {
        return path + ":action";
    }
}
