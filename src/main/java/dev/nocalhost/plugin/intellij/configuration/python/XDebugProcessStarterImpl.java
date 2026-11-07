package dev.nocalhost.plugin.intellij.configuration.python;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.python.pro.debugger.remote.PyRemoteDebugConfiguration;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.python.debugger.PyDebugRunner;
import com.jetbrains.python.debugger.PyRemoteDebugProcess;
import com.jetbrains.python.debugger.remote.vfs.PyRemotePositionConverter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.ServerSocket;

public class XDebugProcessStarterImpl extends XDebugProcessStarter {
    @NotNull
    private final ExecutionEnvironment environment;

    @NotNull
    private final PyRemoteDebugConfiguration conf;

    @NotNull
    private final ExecutionResult result;

    @NotNull
    private final ServerSocket serverSocket;

    public XDebugProcessStarterImpl(
            @NotNull ExecutionEnvironment environment,
            @NotNull PyRemoteDebugConfiguration conf,
            @NotNull ExecutionResult result,
            @NotNull ServerSocket serverSocket
    ) {
        this.environment = environment;
        this.conf = conf;
        this.result = result;
        this.serverSocket = serverSocket;
    }

    @NotNull
    public final ExecutionEnvironment getEnvironment() {
        return this.environment;
    }

    @NotNull
    public final PyRemoteDebugConfiguration getConf() {
        return this.conf;
    }

    @NotNull
    public final ExecutionResult getResult() {
        return this.result;
    }

    @NotNull
    public final ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    @NotNull
    public XDebugProcess start(@NotNull XDebugSession session) {
        PyRemoteDebugProcess pyDebugProcess = new PyRemoteDebugProcess(session, getServerSocket(), getResult().getExecutionConsole(), getResult().getProcessHandler(), getConf().getSettraceCall(getServerSocket().getLocalPort()));
        pyDebugProcess.setPositionConverter(new PyRemotePositionConverter(pyDebugProcess, getConf().getMappingSettings()));
        setupDebugConsole(pyDebugProcess, session);
        return pyDebugProcess;
    }

    /**
     * {@code PyDebugRunner.createConsoleCommunicationAndSetupActions} is present on 261/262 but
     * was removed in IDEA 2026.3 (platform 263), where {@code createConsoleCommunication} is the
     * replacement. Its parameter types changed between platform versions, so resolve the method
     * reflectively to keep one source compiling and running on all supported platforms (261/262/263).
     */
    private void setupDebugConsole(@NotNull PyRemoteDebugProcess process, @NotNull XDebugSession session) {
        final Project project = getEnvironment().getProject();
        final ExecutionResult executionResult = getResult();
        // Match by name and arity: the parameter types of these helpers changed between
        // platform versions, so an exact getMethod(...) lookup is not portable.
        Method setup = findStaticMethod("createConsoleCommunicationAndSetupActions");
        if (setup == null) {
            // IDEA 2026.3+ removed the wrapper; createConsoleCommunication is its replacement.
            setup = findStaticMethod("createConsoleCommunication");
        }
        if (setup == null) {
            throw new IllegalStateException("Failed to set up the Python debug console: no compatible PyDebugRunner method");
        }
        try {
            setup.invoke(null, project, executionResult, process, session);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set up the Python debug console", e);
        }
    }

    @Nullable
    private static Method findStaticMethod(@NotNull String name) {
        for (Method method : PyDebugRunner.class.getMethods()) {
            if (method.getName().equals(name)
                    && method.getParameterCount() == 4
                    && Modifier.isStatic(method.getModifiers())) {
                return method;
            }
        }
        return null;
    }
}
