package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.nocalhost.plugin.intellij.configuration.java.NocalhostJavaConfiguration;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.service.NocalhostDebugReconnectService;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostConsoleManager;

/**
 * 调试断开后重连（只对 Java 远程调试 / JDWP 生效）。
 *
 * <p>网络抖动导致调试连接断开时，容器里的 JVM 通常还在跑（JDWP 端口仍然监听），
 * 这时不需要重新启动远端进程：本动作会让同名的 Debug 运行配置以 attach-only 方式重新启动 ——
 * 恢复调试端口转发后直接 attach 回去，省掉一次远端应用重启。
 *
 * <p>使用入口：
 * <ul>
 *   <li>工作负载右键菜单（DevMode 运行中、且该服务存在 Java 调试配置时显示）；</li>
 *   <li>调试工具窗口工具栏（由 {@code NocalhostJavaDebugRunner} 作为 extra action 挂上）；</li>
 *   <li>Tools 菜单 / Find Action（plugin.xml 中注册的默认实例）。</li>
 * </ul>
 */
public class ReconnectDebugAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(ReconnectDebugAction.class);

    private final @Nullable Project project;
    private final @Nullable String serviceName;

    /** 供 plugin.xml 注册（Tools 菜单 / Find Action）使用：项目从 AnActionEvent 取。 */
    public ReconnectDebugAction() {
        this(null, null);
    }

    public ReconnectDebugAction(@Nullable Project project) {
        this(project, null);
    }

    public ReconnectDebugAction(@Nullable Project project, @Nullable String serviceName) {
        super(NocalhostI18n.get("action.reconnectDebug"),
                NocalhostI18n.get("action.reconnectDebug.description"),
                AllIcons.Actions.Refresh);
        this.project = project;
        this.serviceName = serviceName;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = resolveProject(event);
        boolean enabled = project != null;
        if (enabled && serviceName != null) {
            // 工作负载右键菜单：只有该服务存在 Java 调试运行配置时才可用
            enabled = findSettings(project, serviceName) != null;
        }
        event.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = resolveProject(event);
        if (project == null) {
            return;
        }

        var context = NocalhostContextManager.getInstance(project).getContext();
        String target = StringUtils.isNotEmpty(serviceName)
                ? serviceName
                : (context == null ? null : context.getServiceName());

        RunnerAndConfigurationSettings settings = StringUtils.isEmpty(target) ? null : findSettings(project, target);
        if (settings == null) {
            settings = findSelectedSettings(project);
        }
        if (settings == null) {
            notifyNotConfigured(project, target);
            return;
        }

        var builder = ExecutionEnvironmentBuilder
                .createOrNull(DefaultDebugExecutor.getDebugExecutorInstance(), settings);
        if (builder == null) {
            notifyNotConfigured(project, target);
            return;
        }

        var reconnectService = NocalhostDebugReconnectService.getInstance(project);
        if ( ! reconnectService.tryBeginReattach()) {
            // 已经有一次重连在途（通常是自动重连），不要重复重启会话，否则第二次会退化成完整的 Debug 运行
            LOG.info("[debug] another reattach is in flight, skip manual reconnect");
            return;
        }

        // 用户手动重连：重新给足自动重连的尝试预算
        reconnectService.resetAutoReattachAttempts();
        NocalhostConsoleManager.activateOutputWindow(project);
        RunManager.getInstance(project).setSelectedConfiguration(settings);
        // 关键：告诉下一次 Debug 运行“只 attach 回去，不要执行 nhctl exec 重启远端进程”
        reconnectService.requestReattach();
        try {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build());
        } catch (Exception e) {
            reconnectService.endReattach();
            reconnectService.clearReattachRequest();
            LOG.warn("[debug] fail to restart debug session for reconnect", e);
            NocalhostNotifier
                    .getInstance(project)
                    .notifyError(NocalhostI18n.get("error.reconnectDebug"),
                            NocalhostI18n.get("error.reconnectDebug.content"));
        }
    }

    private void notifyNotConfigured(@NotNull Project project, @Nullable String target) {
        NocalhostNotifier
                .getInstance(project)
                .notifyError(NocalhostI18n.get("error.reconnectDebug"),
                        NocalhostI18n.format("error.reconnectDebug.noConfiguration", StringUtils.trimToEmpty(target)));
    }

    private @Nullable Project resolveProject(@NotNull AnActionEvent event) {
        return project != null ? project : event.getProject();
    }

    private @Nullable RunnerAndConfigurationSettings findSettings(@NotNull Project project, @NotNull String target) {
        for (RunnerAndConfigurationSettings settings : RunManager.getInstance(project).getAllSettings()) {
            if (settings.getConfiguration() instanceof NocalhostJavaConfiguration
                    && StringUtils.equals(settings.getConfiguration().getName(), target)) {
                return settings;
            }
        }
        return null;
    }

    private @Nullable RunnerAndConfigurationSettings findSelectedSettings(@NotNull Project project) {
        RunnerAndConfigurationSettings settings = RunManager.getInstance(project).getSelectedConfiguration();
        if (settings != null && settings.getConfiguration() instanceof NocalhostJavaConfiguration) {
            return settings;
        }
        return null;
    }
}
