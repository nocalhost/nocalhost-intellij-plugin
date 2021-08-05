package dev.nocalhost.plugin.intellij.task;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
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
        super(project, String.format("Starting %s", debug ? "Debug" : "Run"), false);
        this.debug = debug;
        this.project = project;
        this.service = service;
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        LOG.error("error occurred while starting dev mode", e);
        NocalhostNotifier.getInstance(project).notifyError(
                "Nocalhost starting dev mode error",
                "Error occurred while starting dev mode",
                e.getMessage());
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
            var resp = cmd.syncStatus(service.getApplicationName(), opts);
            var json = DataUtils.GSON.fromJson(resp, NhctlSyncStatus.class);

            // TODO
            LOG.debug(resp);
            if ("idle".equals(json.getStatus())) {
                ExecuteTask.this.doRun();
                break;
            }
            Thread.sleep(3000);
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

    private Class<? extends ConfigurationType> getConfType() {
        var ide = ApplicationInfo.getInstance().getBuild().getProductCode();
        // TODO
        ide = "PY";
        return hash.containsKey(ide) ? hash.get(ide) : null;
    }

    protected RunnerAndConfigurationSettings getConf() {
        RunnerAndConfigurationSettings conf;
        var manager = RunManager.getInstance(project);
        var type = getConfType();
        var list = manager.getConfigurationSettingsList(type);
        if (list.isEmpty()) {
            conf = manager.createConfiguration("demo", type);
            manager.addConfiguration(conf);
        } else {
            conf = list.get(0);
        }
        // TODO
        if (conf.getConfiguration() instanceof NocalhostPythonConfiguration) {
            ((NocalhostPythonConfiguration) conf.getConfiguration()).setPort(9004);
        }
        manager.setSelectedConfiguration(conf);
        return conf;
    }
}
