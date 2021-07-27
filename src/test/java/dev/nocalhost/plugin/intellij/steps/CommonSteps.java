package dev.nocalhost.plugin.intellij.steps;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.utils.Keyboard;

import java.time.Duration;

import dev.nocalhost.plugin.intellij.pages.DialogFixture;
import dev.nocalhost.plugin.intellij.pages.IdeaFrame;
import dev.nocalhost.plugin.intellij.pages.WelcomeFrameFixture;
import kotlin.Unit;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static dev.nocalhost.plugin.intellij.pages.DialogFixture.byTitle;

public class CommonSteps {
    private final RemoteRobot remoteRobot;
    private final Keyboard keyboard;

    public CommonSteps(RemoteRobot remoteRobot) {
        this.remoteRobot = remoteRobot;
        this.keyboard = new Keyboard(remoteRobot);
    }

    public void createProject() {
        step("Create New Command Line Project", () -> {
            final WelcomeFrameFixture welcomeFrame = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(10));
            welcomeFrame.createNewProjectLink().click();

            final DialogFixture newProjectDialog = welcomeFrame.find(DialogFixture.class, DialogFixture.byTitle("New Project"), Duration.ofSeconds(20));
            newProjectDialog.findText("Java").click();
            newProjectDialog.button("Next").click();
            newProjectDialog.button("Next").click();
            newProjectDialog.button("Finish").click();
        });
    }

    public void closeTipOfTheDay() {
        step("Close Tip of the Day if it appears", () -> {
            waitFor(Duration.ofSeconds(20), () -> remoteRobot.findAll(DialogFixture.class, byXpath("//div[@class='MyDialog'][.//div[@text='Running startup activities...']]")).size() == 0);
            final IdeaFrame idea = remoteRobot.find(IdeaFrame.class);
            idea.dumbAware(() -> {
                try {
                    idea.find(DialogFixture.class, byTitle("Tip of the Day")).button("Close").click();
                } catch (Throwable ignore) {
                }
                return Unit.INSTANCE;
            });
        });
    }

    public void closeCodeWithMe() {
        step("Close Code With Me Popup", () -> {
            final IdeaFrame idea = remoteRobot.find(IdeaFrame.class);
            ComponentFixture gotItButtonFixture = idea.find(ComponentFixture.class, byXpath("//div[@class='JButton' and @text='Got It']"));
            if (gotItButtonFixture.isShowing()) {
                gotItButtonFixture.click();
            }
        });
    }

}
