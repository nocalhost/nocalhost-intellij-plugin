package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.application.ApplicationManager;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SudoUtil {
    public static List<String> toSudoCommand(List<String> args) {
        return Stream
                .concat(List.of("sudo", "--reset-timestamp", "--stdin").stream(), args.stream())
                .collect(Collectors.toList());
    }

    public static void inputPassword(Process process, String password) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            PrintWriter pw = new PrintWriter(process.getOutputStream());
            pw.println(password);
            pw.flush();
            pw.close();
        });
    }
}
