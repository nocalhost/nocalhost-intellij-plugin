package dev.nocalhost.plugin.intellij.service;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 调试“断线重连”的共享状态。
 *
 * <p>网络抖动导致调试连接断开时，容器里的 JVM 往往还在跑（JDWP 端口仍然监听），
 * 这时不需要重启远端进程，只需要重新建立端口转发并 attach 回去即可。
 * 本服务在“触发方”（用户点击 / 自动重连）与“执行方”（{@code NocalhostProfileState} /
 * {@code NocalhostJavaDebugRunner}）之间传递这些信号：
 * <ul>
 *   <li>{@link #requestReattach()} / {@link #consumeReattachRequest()}：
 *       让下一次 Debug 运行走 attach-only 分支（不执行 {@code nhctl exec}，不重启远端进程）；</li>
 *   <li>{@link #tryBeginReattach()} / {@link #endReattach()}：
 *       同一时刻只允许一次重连在途，避免自动重连与手动重连互相覆盖，
 *       导致第二次重启退化成“普通 Debug 运行”（重新拉起远端进程）；</li>
 *   <li>{@link #beginReattachSession()} / {@link #endReattachSession()}：
 *       记录当前是否有 attach-only 会话持有调试端口转发，其它会话退出时不要把它断开；</li>
 *   <li>{@link #suppressAutoReconnect(String, long)}：插件自己发起的重启
 *       （重新运行 / 结束 DevMode / 热加载）期间不要自动重连，避免和用户的显式操作打架。</li>
 * </ul>
 */
public class NocalhostDebugReconnectService {
    /**
     * 重连请求的有效期：Debug 会话会在很短时间内消费该请求，
     * 超过这个时间还没被消费说明这次请求已经过期（例如用户取消了 prepare），丢弃即可。
     */
    private static final long REQUEST_TTL_MILLIS = 20_000L;

    /** 插件自己发起重启后，默认抑制自动重连的时间窗口。 */
    private static final long DEFAULT_SUPPRESS_MILLIS = 15_000L;

    /** “重连在途”标记的兜底有效期，避免异常路径下标记一直被占住。 */
    private static final long REATTACH_IN_FLIGHT_TTL_MILLIS = 60_000L;

    /** 自动重连的最大连续次数：超过后不再自动重连（仍然可以手动点击“重连调试”）。 */
    private static final int MAX_AUTO_REATTACH_ATTEMPTS = 5;

    private final AtomicLong requestedAt = new AtomicLong(0L);
    private final AtomicLong suppressedUntil = new AtomicLong(0L);
    private final AtomicLong reattachInFlightSince = new AtomicLong(0L);
    private final AtomicInteger reattachSessions = new AtomicInteger(0);
    private final AtomicInteger autoReattachAttempts = new AtomicInteger(0);

    public static NocalhostDebugReconnectService getInstance(@NotNull Project project) {
        return project.getService(NocalhostDebugReconnectService.class);
    }

    /** 请求下一次 Debug 运行以 attach-only（重连）方式启动。 */
    public void requestReattach() {
        requestedAt.set(System.currentTimeMillis());
    }

    /**
     * 消费一次重连请求。返回 true 表示当前这次 Debug 运行应该走 attach-only 分支。
     * 只在 Java 调试流程下调用，避免“运行”或其它语言（Go/Node/PHP）的调试流程误消费。
     */
    public boolean consumeReattachRequest() {
        long at = requestedAt.getAndSet(0L);
        return at > 0L && System.currentTimeMillis() - at <= REQUEST_TTL_MILLIS;
    }

    /** 丢弃尚未被消费的重连请求（例如这次重启被抑制或失败）。 */
    public void clearReattachRequest() {
        requestedAt.set(0L);
    }

    /**
     * 申请发起一次重连（手动或自动）。同一时刻只允许一次，
     * 否则第二次 {@code restartRunProfile} 会因为请求已被消费而退化成完整的 Debug 运行，
     * 反而把远端进程重启一遍。
     *
     * @return true 表示可以发起；false 表示已有重连在途，本次应放弃
     */
    public boolean tryBeginReattach() {
        long now = System.currentTimeMillis();
        long since = reattachInFlightSince.get();
        if (since > 0L && now - since < REATTACH_IN_FLIGHT_TTL_MILLIS) {
            return false;
        }
        reattachInFlightSince.set(now);
        return true;
    }

    /** 重连已经交给平台执行（会话启动成功或失败），释放“在途”标记。 */
    public void endReattach() {
        reattachInFlightSince.set(0L);
    }

    /**
     * 记一次 attach-only 会话开始。这些会话不会主动断开调试端口转发，
     * 其它会话在它们退出期间也不应该断开（否则下一次重连会用到一个刚被拆掉的隧道）。
     */
    public void beginReattachSession() {
        reattachSessions.incrementAndGet();
    }

    public void endReattachSession() {
        reattachSessions.updateAndGet(value -> value > 0 ? value - 1 : 0);
    }

    /** 当前是否有 attach-only（重连）会话持有着调试端口转发。 */
    public boolean hasActiveReattachSession() {
        return reattachSessions.get() > 0;
    }

    /**
     * 申请一次自动重连机会。
     *
     * @return 本次是第几次自动重连；返回负数表示已经超过上限，应该放弃自动重连
     */
    public int nextAutoReattachAttempt() {
        int attempt = autoReattachAttempts.incrementAndGet();
        return attempt <= MAX_AUTO_REATTACH_ATTEMPTS ? attempt : -1;
    }

    /** 上一次连接是稳定存活过的（或者用户手动重连成功），重新给足重试预算。 */
    public void resetAutoReattachAttempts() {
        autoReattachAttempts.set(0);
    }

    /** 插件自己发起的重启（重跑、热加载等），此时不要自动重连。 */
    public void suppressAutoReconnect(@NotNull String reason) {
        suppressAutoReconnect(reason, DEFAULT_SUPPRESS_MILLIS);
    }

    /**
     * 插件自己发起的重启。{@code millis} 用于覆盖耗时较长的操作（例如 {@code nhctl dev end}）。
     */
    public void suppressAutoReconnect(@NotNull String reason, long millis) {
        suppressedUntil.set(System.currentTimeMillis() + Math.max(millis, 0L));
    }

    /** 当前是否处于“不要自动重连”的时间窗口内。 */
    public boolean isAutoReconnectSuppressed() {
        return System.currentTimeMillis() < suppressedUntil.get();
    }
}
