package dev.nocalhost.plugin.intellij;

public class TestService {

    public TestService() {
        System.out.println("Service " + getClass().getClassLoader().getClass().getSimpleName());
        System.out.println("Loaded from " + getClass().getProtectionDomain().getCodeSource().getLocation());
    }
}
