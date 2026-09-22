package dev.nocalhost.plugin.intellij.utils;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * JDWP 探活工具。
 *
 * <p>JDWP 的握手协议非常简单：客户端先发送 14 个字节的 {@code JDWP-Handshake}，
 * 调试目标会原样回显同样 14 个字节。因此只要回显一致，就说明对端确实有一个
 * 活着的 JVM 调试端口在监听 —— 这正是“断线重连”需要的前置条件。
 *
 * <p>如果端口转发还在、但远端进程已经退出（比如 DevMode 已经结束），
 * 握手会失败，插件就能给出明确的提示，而不是让用户去猜为什么 attach 不上。
 */
public final class JdwpUtil {
    public static final String JDWP_HANDSHAKE = "JDWP-Handshake";

    private static final byte[] HANDSHAKE_BYTES = JDWP_HANDSHAKE.getBytes(UTF_8);

    private JdwpUtil() {
    }

    /**
     * 探测给定地址上是否有存活的 JDWP 监听。
     *
     * @param host          调试目标地址，通常是 127.0.0.1
     * @param port          本地调试端口（端口转发后的端口）
     * @param timeoutMillis 连接与读取的超时时间
     * @return true 表示握手成功，对端 JVM 还活着
     */
    public static boolean isJdwpListenerAlive(@NotNull String host, String port, int timeoutMillis) {
        if (StringUtils.isEmpty(port)) {
            return false;
        }
        int portNumber;
        try {
            portNumber = Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (portNumber <= 0 || portNumber > 65535) {
            return false;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, portNumber), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            OutputStream output = socket.getOutputStream();
            output.write(HANDSHAKE_BYTES);
            output.flush();

            byte[] response = new byte[HANDSHAKE_BYTES.length];
            InputStream input = socket.getInputStream();
            int read = 0;
            while (read < response.length) {
                int count = input.read(response, read, response.length - read);
                if (count < 0) {
                    break;
                }
                read += count;
            }
            if (read != response.length) {
                return false;
            }
            for (int i = 0; i < response.length; i++) {
                if (response[i] != HANDSHAKE_BYTES[i]) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            // 连接被拒绝 / 超时 / 端口转发是死的 —— 都视为“暂时连不上”
            return false;
        }
    }
}
