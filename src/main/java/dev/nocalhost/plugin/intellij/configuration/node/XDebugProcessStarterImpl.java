package dev.nocalhost.plugin.intellij.configuration.node;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class XDebugProcessStarterImpl extends XDebugProcessStarter {
    private static final long POLL_TIMEOUT = 10 * 60 * 1000;

    @NotNull
    private final ExecutionEnvironment environment;

    @NotNull
    private final NocalhostNodeConfiguration conf;

    @NotNull
    private final InetSocketAddress socket;

    @NotNull
    private final ExecutionResult result;

    public XDebugProcessStarterImpl(
            @NotNull ExecutionEnvironment environment,
            @NotNull NocalhostNodeConfiguration conf,
            @NotNull InetSocketAddress socket,
            @NotNull ExecutionResult result
    ) {
        this.environment = environment;
        this.conf = conf;
        this.result = result;
        this.socket = socket;
    }

    @Override
    public @NotNull XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
        final long startTime = System.currentTimeMillis();
        // https://github.com/nodejs/node/issues/9617
        final String url = "http://127.0.0.1:" + conf.getPort() + "/json";
        final Request request = new Request.Builder().url(url).get().build();
        final OkHttpClient client = new OkHttpClient();

        while (true) {
            if (System.currentTimeMillis() - startTime > POLL_TIMEOUT) {
                throw new ExecutionException("The attempt to connect to the remote debug port timed out.");
            }
            try {
                Thread.sleep(500);
                var response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    break;
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        return conf.createDebugProcess(socket, session, result, environment);
    }
}
