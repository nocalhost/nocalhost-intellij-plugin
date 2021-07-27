package dev.nocalhost.plugin.intellij;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.fixtures.JTextAreaFixture;
import com.intellij.remoterobot.utils.Keyboard;

import org.assertj.swing.core.MouseButton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.pages.DialogFixture;
import dev.nocalhost.plugin.intellij.pages.IdeaFrame;
import dev.nocalhost.plugin.intellij.steps.CommonSteps;
import dev.nocalhost.plugin.intellij.utils.RemoteRobotExtension;
import dev.nocalhost.plugin.intellij.utils.StepsLogger;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static dev.nocalhost.plugin.intellij.pages.ActionMenuFixtureKt.actionMenu;
import static dev.nocalhost.plugin.intellij.pages.ActionMenuFixtureKt.actionMenuItem;
import static dev.nocalhost.plugin.intellij.pages.DialogFixture.byTitle;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_META;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.time.Duration.ofMinutes;

@ExtendWith(RemoteRobotExtension.class)
public class ConnectClusterTest {
    private static String kubeConfigText;

    static {
        try {
            kubeConfigText = Files.readString(Paths.get(System.getProperty("user.home"), ".kube/config"));
        } catch (Exception ignored) {
        }
    }

    private final RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8082");
    private final CommonSteps commonSteps = new CommonSteps(remoteRobot);
    private final Keyboard keyboard = new Keyboard(remoteRobot);

    @BeforeAll
    public static void initLogging() {
        StepsLogger.init();
    }

    @BeforeEach
    public void createProject() {
        commonSteps.createProject();

        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class);
        waitFor(ofMinutes(5), () -> !idea.isDumbMode());

        commonSteps.closeTipOfTheDay();

        commonSteps.closeCodeWithMe();

        idea.find(ComponentFixture.class, byXpath("//div[@class='StripeButton' and @text='Nocalhost']")).click();
    }

    @AfterEach
    public void closeProject(final RemoteRobot remoteRobot) {
        step("Close the project", () -> {
            if (remoteRobot.isMac()) {
                keyboard.hotKey(VK_SHIFT, VK_META, VK_A);
                keyboard.enterText("Close Project");
                keyboard.enter();
            } else {
                actionMenu(remoteRobot, "File").click();
                actionMenuItem(remoteRobot, "Close Project").click();
            }
        });
    }

    @Test
    public void connectCluster(final RemoteRobot remoteRobot) {
        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class);

        step("Connect cluster by default kubeconfig", () -> {
            idea.find(ComponentFixture.class, byXpath("//div[@accessiblename='Add Standalone Clusters' and @class='ActionButton' and @myaction='Add Standalone Clusters ()']")).click();
            idea.find(DialogFixture.class, byTitle("Add Standalone Clusters")).button("Add").click();
            ContainerFixture nocalhostTreeFixture = idea.find(ContainerFixture.class, byXpath("//div[@class='NocalhostTree']"));
            waitFor(ofMinutes(1), () -> nocalhostTreeFixture.hasText("docker-desktop"));
            nocalhostTreeFixture.findText("docker-desktop").click(MouseButton.RIGHT_BUTTON);
            actionMenuItem(remoteRobot, "Remove").click();
        });

        step("Connect cluster by kubeconfig text", () -> {
            idea.find(ComponentFixture.class, byXpath("//div[@accessiblename='Add Standalone Clusters' and @class='ActionButton' and @myaction='Add Standalone Clusters ()']")).click();
            DialogFixture dialogFixture = idea.find(DialogFixture.class, byTitle("Add Standalone Clusters"));
            dialogFixture.findText("Paste as Text").click();
            dialogFixture.find(ComponentFixture.class, byXpath("//div[@class='JBTextArea']")).click();
            keyboard.enterText(kubeConfigText);
            waitFor(ofMinutes(1), () -> dialogFixture.hasText("docker-desktop"));
            dialogFixture.button("Add").click();
            ContainerFixture nocalhostTreeFixture = idea.find(ContainerFixture.class, byXpath("//div[@class='NocalhostTree']"));
            waitFor(ofMinutes(1), () -> nocalhostTreeFixture.hasText("docker-desktop"));
            nocalhostTreeFixture.findText("docker-desktop").click(MouseButton.RIGHT_BUTTON);
            actionMenuItem(remoteRobot, "Remove").click();
        });
    }
}
