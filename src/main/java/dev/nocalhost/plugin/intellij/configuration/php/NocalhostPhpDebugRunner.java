package dev.nocalhost.plugin.intellij.configuration.php;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.php.PhpBundle;
import com.jetbrains.php.config.PhpProjectConfigurationFacade;
import com.jetbrains.php.config.servers.PhpServer;
import com.jetbrains.php.debug.PhpDebugExtension;
import com.jetbrains.php.debug.PhpDebugUtil;
import com.jetbrains.php.debug.common.PhpDebugProcess;
import com.jetbrains.php.debug.common.PhpDebugProcessFactory;
import com.jetbrains.php.debug.connection.PhpDebugConnectionManager;
import com.jetbrains.php.debug.listener.PhpDebugExternalConnectionsAccepter;
import com.jetbrains.php.run.PhpDebugRunner;
import com.jetbrains.php.run.remoteDebug.PhpRemoteDebugDebugRunner;
import com.jetbrains.php.run.remoteDebug.PhpRemoteDebugRunConfiguration;
import com.jetbrains.php.run.remoteDebug.PhpRemoteDebugRunConfiguration.Settings;
import com.jetbrains.php.util.connection.PhpIncomingDebugConnectionServer;
import javax.swing.event.HyperlinkEvent.EventType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import dev.nocalhost.plugin.intellij.configuration.NocalhostProfileState;
import dev.nocalhost.plugin.intellij.configuration.NocalhostRunner;

public class NocalhostPhpDebugRunner extends PhpDebugRunner<NocalhostPhpConfiguration> {
    public NocalhostPhpDebugRunner() {
        super(NocalhostPhpConfiguration.class);
    }

    @NotNull
    public String getRunnerId() {
        return "NocalhostPhpDebugRunner";
    }

    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        RunProfile profile = environment.getRunProfile();
        if (profile instanceof PhpRemoteDebugRunConfiguration) {
            PhpRemoteDebugRunConfiguration configuration = (PhpRemoteDebugRunConfiguration)profile;
            Settings settings = configuration.getSettings();
            if (StringUtil.isEmpty(settings.getServerName())) {
                final Project project = configuration.getProject();
                NotificationListener listener = (notification, event) -> {
                    if (event.getEventType() == EventType.ACTIVATED) {
                        String description = event.getDescription();
                        if ("zero_config".equals(description)) {
                            BrowserUtil.browse("https://www.jetbrains.com/help/phpstorm/zero-configuration-debugging.html", project);
                        } else if ("stop".equals(description)) {
                            notification.expire();
                            PhpDebugExternalConnectionsAccepter.getInstance(project).doSwitch();
                            String title = PhpBundle.message("PhpRemoteDebugRunConfigurationEditor.stop.zero.configuration.title", new Object[0]);
                            String message = PhpBundle.message("PhpRemoteDebugRunConfigurationEditor.stop.zero.configuration", new Object[0]);
                            PhpDebugUtil.showInformationBalloon(project, title, message, (NotificationListener)null);
                        }
                    }
                };
                boolean isStarted = PhpDebugExternalConnectionsAccepter.getInstance(project).isStarted();
                String title;
                String message;
                if (isStarted) {
                    title = PhpBundle.message("PhpRemoteDebugRunConfigurationEditor.zero.configuration.used.title", new Object[0]);
                    message = PhpBundle.message("PhpRemoteDebugRunConfigurationEditor.zero.configuration.is.already.used", new Object[0]);
                    PhpDebugUtil.showInformationBalloon(project, title, message, listener);
                } else {
                    ((NocalhostProfileState)state).prepareDevInfo();
                    NocalhostRunner runner = Objects.requireNonNull(ProgramRunner.PROGRAM_RUNNER_EP.findExtension(NocalhostRunner.class));
                    state.execute(environment.getExecutor(), runner);

                    PhpDebugExternalConnectionsAccepter.getInstance(project).doSwitch();
                    title = PhpBundle.message("PhpRemoteDebugRunConfigurationEditor.zero.configuration.used.title", new Object[0]);
                    message = PhpBundle.message("PhpRemoteDebugRunConfigurationEditor.zero.configuration.used", new Object[]{configuration.getName()});
                    PhpDebugUtil.showInformationBalloon(project, title, message, listener);
                }

                return null;
            }
        }

        return super.doExecute(state, environment);
    }

    protected RunContentDescriptor doExecute(@NotNull NocalhostPhpConfiguration configuration, @NotNull RunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
        Project project = configuration.getProject();
        Settings settings = configuration.getSettings();
        PhpServer server = PhpProjectConfigurationFacade.getInstance(project).findServer(settings.getServerName());

        assert server != null;

        final String serverName = server.getName();

        assert serverName != null;

        String debuggerId = server.getDebuggerId();
        if (!PhpDebugUtil.isXDebug(debuggerId) && !PhpDebugUtil.isZendDebugger(debuggerId)) {
            throw new ExecutionException(PhpBundle.message("unknown.debugger.type", new Object[0]));
        } else {
            final PhpDebugExtension debugExtension = PhpDebugUtil.getDebugExtensionByDebuggerId(debuggerId);
            final PhpDebugConnectionManager connectionsManager = debugExtension.createDebugConnectionManager();
            Ref<XDebugSession> startedSession = new Ref();
            final PhpIncomingDebugConnectionServer debugServer = debugExtension.startDebugServer(project, (error) -> {
                if (StringUtil.isNotEmpty(error)) {
                    ProgramRunnerUtil.handleExecutionError(project, env, new ExecutionException(error), env.getRunProfile());
                    XDebugSession session = (XDebugSession)startedSession.get();
                    if (session != null) {
                        session.stop();
                    }
                }

            });
            final String sessionId = settings.getSessionId();
            debugServer.registerSessionHandler(sessionId, true, connectionsManager);

            try {
                XDebugSession debugSession = XDebuggerManager.getInstance(project).startSession(env, new XDebugProcessStarter() {
                    @NotNull
                    public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
                        session.addSessionListener(new XDebugSessionListener() {
                            public void sessionStopped() {
                                debugServer.unregisterSessionHandler(sessionId);
                            }
                        });
                        PhpDebugProcess var10000 = PhpDebugProcessFactory.forRemoteDebug(session, sessionId, connectionsManager, serverName, debugExtension.getDebugDriver());
                        if (var10000 == null) {
                            throw new ExecutionException("`process` is null");
                        }

                        return var10000;
                    }
                });
                startedSession.set(debugSession);
                return debugSession.getRunContentDescriptor();
            } catch (ExecutionException var15) {
                debugServer.unregisterSessionHandler(sessionId);
                throw var15;
            }
        }
    }
}
