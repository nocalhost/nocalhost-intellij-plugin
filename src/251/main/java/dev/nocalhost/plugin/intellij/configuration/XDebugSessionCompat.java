package dev.nocalhost.plugin.intellij.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebuggerManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 启动 XDebug 会话，并返回需要回传给 {@code ExecutionManager#startRunProfile} 的 RunContentDescriptor。
 *
 * <p>各平台版本的实现不同：平台 261（2026.1）起
 * {@code XDebugSession#getRunContentDescriptor()} 已废弃，且在 split debugger 模式下会直接报错
 * “[Split debugger] RunContentDescriptor should not be used in split mode from XDebugSession”，
 * 必须改用 {@code XDebuggerManager#newSessionBuilder(XDebugProcessStarter)}；
 * 241 - 253 没有 newSessionBuilder，只能继续使用
 * {@code XDebuggerManager#startSession(ExecutionEnvironment, XDebugProcessStarter)}。
 *
 * <p>该类与 OpenProjectExecutor 等类一样，在每个平台版本目录下各有一份实现，
 * 编译时只会使用当前 platformVersion 对应的那一份，详见 build.gradle.kts 的 sourceSets 配置。
 */
public final class XDebugSessionCompat {

    private XDebugSessionCompat() {
    }

    @Nullable
    public static RunContentDescriptor startSession(@NotNull ExecutionEnvironment environment,
                                                    @NotNull XDebugProcessStarter starter) throws ExecutionException {
        return XDebuggerManager.getInstance(environment.getProject())
                .startSession(environment, starter)
                .getRunContentDescriptor();
    }
}
