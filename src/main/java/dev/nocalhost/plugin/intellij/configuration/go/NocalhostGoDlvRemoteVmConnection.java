package dev.nocalhost.plugin.intellij.configuration.go;

import com.goide.dlv.DlvDisconnectOption;
import com.goide.dlv.DlvRemoteVmConnection;
import com.goide.dlv.DlvVm;
import com.goide.dlv.protocol.DlvApi;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.intellij.openapi.util.Condition;
import com.intellij.util.io.socketConnection.ConnectionStatus;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NocalhostGoDlvRemoteVmConnection extends DlvRemoteVmConnection {
    private static final long POLL_TIMEOUT = 10 * 60 * 1000;    // 10 minutes

    public NocalhostGoDlvRemoteVmConnection(@NotNull DlvDisconnectOption disconnectOption) {
        super(disconnectOption);
    }

    @Override
    protected void doOpen(@NotNull AsyncPromise<DlvVm> result,
                          @NotNull InetSocketAddress address,
                          @Nullable Condition<Void> stopCondition) {
        final long startTime = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - startTime > POLL_TIMEOUT) {
                close("Wait for remote debug port timeout", ConnectionStatus.DISCONNECTED);
                return;
            }

            Socket socket = null;
            try {
                Thread.sleep(500);
                socket = new Socket(address.getAddress(), address.getPort());
                JsonRpcClient client = new JsonRpcClient();
                client.invokeAndReadResponse(
                        "RPCServer.GetVersion",
                        null,
                        DlvApi.GetVersion.class,
                        socket.getOutputStream(),
                        socket.getInputStream());
                break;
            } catch (Throwable e) {
                if (StringUtils.contains(e.getMessage(),
                        "No content to map due to end-of-input")) {
                    continue;
                }
                if (StringUtils.contains(e.getMessage(), "Connection reset")) {
                    continue;
                }
                String msg = "Fail to connect remote debug port: " + e.getMessage();
                close(msg, ConnectionStatus.DISCONNECTED);
                return;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }

        super.doOpen(result, address, stopCondition);
    }
}
