package dev.nocalhost.plugin.intellij.task;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
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
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class ExecuteTask extends Task.Backgroundable {
    protected final boolean debug;
    protected final Project project;
    protected final ServiceProjectPath service;
    private static final Logger LOG = Logger.getInstance(ExecuteTask.class);

    private final Map<String, Class<? extends ConfigurationType>> hash = new HashMap<>() {
        {
            put("IU", NocalhostJavaConfigurationType.class);
            put("GO", NocalhostGoConfigurationType.class);
            put("PS", NocalhostPhpConfigurationType.class);
            put("PY", NocalhostPythonConfigurationType.class);
        }
    };

    public ExecuteTask(Project project, ServiceProjectPath service, boolean debug) {
        super(project, String.format("Starting %s", debug ? "`Debug`" : "`Run`"), false);
        this.debug = debug;
        this.project = project;
        this.service = service;
    }

    @Override
    public void onThrowable(@NotNull Throwable ex) {
        LOG.error(String.format("error occurred while starting %s", debug ? "`Debug`" : "`Run`"), ex);
        NocalhostNotifier.getInstance(project).notifyError(
                "Nocalhost",
                String.format("error occurred while starting %s", debug ? "`Debug`" : "`Run`"),
                ex.getMessage()
        );
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        while (true) {
            var path = KubeConfigUtil.kubeConfigPath(service.getRawKubeConfig());
            var opts = new NhctlSyncStatusOptions(path, service.getNamespace());
            opts.setDeployment(service.getServiceName());
            opts.setControllerType(service.getServiceType());
            var cmd = project.getService(OutputCapturedNhctlCommand.class);
            var text = cmd.syncStatus(service.getApplicationName(), opts);
            var json = DataUtils.GSON.fromJson(text, NhctlSyncStatus.class);

            // TODO
            LOG.debug(text);
            if ("idle".equals(json.getStatus())) {
                doRun();
                break;
            }
            Thread.sleep(5000);
        }
    }

    protected void doRun() throws InterruptedException {
        // kill active session
        var processes = ExecutionManager.getInstance(project).getRunningProcesses();
        if (processes.length > 0) {
            Arrays.stream(processes).forEach(x -> x.destroyProcess());
            Thread.sleep(1000);
        }
        // start new session
        var executor = debug
                ? DefaultDebugExecutor.getDebugExecutorInstance()
                : DefaultRunExecutor.getRunExecutorInstance();
        var builder = ExecutionEnvironmentBuilder.createOrNull(executor, getConf());
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build());
        }
    }

    private @Nullable Class<? extends ConfigurationType> getConfType() {
        var ide = ApplicationInfo.getInstance().getBuild().getProductCode();
        return hash.containsKey(ide) ? hash.get(ide) : null;
    }

    private @Nullable String resolveDebugPort(@NotNull ServiceProjectPath service) throws ExecutionException, InterruptedException, NocalhostExecuteCmdException, IOException {
        var cmd = ApplicationManager.getApplication().getService(NhctlCommand.class);
        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(service.getRawKubeConfig());
        NhctlConfigOptions opts = new NhctlConfigOptions(kubeConfigPath, service.getNamespace());
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
        if (conf.getConfiguration() instanceof NocalhostPythonConfiguration) {
            var port = resolveDebugPort(service);
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
}
