package dev.nocalhost.plugin.intellij.task;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlAppPortForward;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardListOptions;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.Condition;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import lombok.SneakyThrows;

import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_RAW_MANIFEST;

public class InstallQuickDemoTask extends Task.Backgroundable {
    private static final String DEMO_NAME = "bookinfo";
    private static final String DEMO_GIT_URL = "https://github.com/nocalhost/bookinfo.git";

    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private AtomicReference<String> productPagePort = new AtomicReference<>(null);

    public InstallQuickDemoTask(Project project, Path kubeConfigPath, String namespace) {
        super(project, "Install quick demo", false);
        this.project = project;
        this.kubeConfigPath = kubeConfigPath;
        this.namespace = namespace;
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void onSuccess() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

        NocalhostNotifier.getInstance(project).notifySuccess("Quick demo installed", "");

        if (StringUtils.isNotEmpty(productPagePort.get())) {
            BrowserUtil.browse("http://127.0.0.1:" + productPagePort.get() + "/productpage");
        }
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        NocalhostNotifier.getInstance(project).notifyError("Quick demo install error",
                "Error occurred while installing quick demo", e.getMessage());
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        NhctlInstallOptions nhctlInstallOptions = new NhctlInstallOptions(kubeConfigPath, namespace);
        nhctlInstallOptions.setGitUrl(DEMO_GIT_URL);
        nhctlInstallOptions.setType(MANIFEST_TYPE_RAW_MANIFEST);
        outputCapturedNhctlCommand.install(DEMO_NAME, nhctlInstallOptions);

        NhctlPortForwardListOptions nhctlPortForwardListOptions = new NhctlPortForwardListOptions(
                kubeConfigPath, namespace);
        List<NhctlAppPortForward> portForwards = nhctlCommand.listPortForward(DEMO_NAME,
                nhctlPortForwardListOptions);
        for (NhctlAppPortForward portForward : portForwards) {
            if (portForward.getPort().endsWith(":9080")) {
                int pos = portForward.getPort().lastIndexOf(":9080");
                productPagePort.set(portForward.getPort().substring(0, pos));
                break;
            }
        }

        int total;
        int ready;
        do {
            Thread.sleep(3000);

            NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(kubeConfigPath, namespace);
            nhctlGetOptions.setApplication(DEMO_NAME);
            List<NhctlGetResource> resources = nhctlCommand.getResources("Deployment", nhctlGetOptions);
            total = resources.size();
            ready = 0;

            for (NhctlGetResource resource : resources) {
                List<Condition> conditions = resource.getKubeResource().getStatus().getConditions();
                if (conditions == null) {
                    continue;
                }
                for (Condition condition : conditions) {
                    if (StringUtils.equals(condition.getType(), "Available")
                            && StringUtils.equals(condition.getStatus(), "True")) {
                        ready++;
                        break;
                    }
                }
            }
        } while (total != ready);
    }
}
