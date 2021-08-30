package dev.nocalhost.plugin.intellij.exception.report;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsActions;
import com.intellij.util.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Properties;

import dev.nocalhost.plugin.intellij.Version;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;

public class NocalhostErrorReportSubmitter extends ErrorReportSubmitter {

    private static final SentryClient sentryClient;

    static {
        InputStream in = NocalhostErrorReportSubmitter.class.getClassLoader().getResourceAsStream("config.properties");
        Properties properties = new Properties();
        String dsn = "";
        try {
            properties.load(in);
            dsn = properties.getProperty("sentryDsn");
        } catch (IOException ignore) {
        }
        sentryClient = SentryClientFactory.sentryClient(dsn);
    }

    @Override
    public @NotNull @NlsActions.ActionText String getReportActionText() {
        return "Report to Nocalhost Developer";
    }

    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        Error error = generateError(events[0], additionalInfo);
        DataContext context = DataManager.getInstance().getDataContext(parentComponent);
        Project project = CommonDataKeys.PROJECT.getData(context);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Sending error report") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                postError(error);
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showInfoMessage(parentComponent, "Thank you for submitting your report!", "Error Report");
                    consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
                });
            }
        });
        return true;
    }

    private void postError(Error error) {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.withLevel(Event.Level.ERROR);
        eventBuilder.withTimestamp(new Timestamp(System.currentTimeMillis()));
        eventBuilder.withRelease(error.getPluginVersion());
        eventBuilder.withTag("PluginVersion", error.getPluginVersion());
        eventBuilder.withTag("IntelliJVersion", error.getIntellijVersion());
        eventBuilder.withTag("os", error.getOs());
        eventBuilder.withTag("Java", error.getJava());
        eventBuilder.withMessage(error.getExceptionMessage());
        eventBuilder.withExtra("Addition", error.getAdditionInfo());
        eventBuilder.withExtra("Exception", error.getException());

        Event event = eventBuilder.build();

        sentryClient.sendEvent(event);
    }

    private Error generateError(IdeaLoggingEvent loggingEvent, String additionalInfo) {
        Error error = new Error();
        error.setAdditionInfo(additionalInfo);
        error.setPluginVersion(Version.get());
        ApplicationInfoEx applicationInfoEx = ApplicationInfoEx.getInstanceEx();
        String intellijVersion = String.format("%s %s.%s %s",
                applicationInfoEx.getVersionName(), applicationInfoEx.getMajorVersion(), applicationInfoEx.getMinorVersion(), applicationInfoEx.getApiVersion());
        error.setIntellijVersion(intellijVersion);
        error.setOs(String.format("%s %s", System.getProperty("os.name"), System.getProperty("os.version")));
        error.setJava(String.format("%s %s", System.getProperty("java.vendor"), System.getProperty("java.version")));
        error.setException(loggingEvent.getThrowableText());
        error.setExceptionMessage(loggingEvent.getMessage());
        error.setThrowable(loggingEvent.getThrowable());
        return error;
    }
}
