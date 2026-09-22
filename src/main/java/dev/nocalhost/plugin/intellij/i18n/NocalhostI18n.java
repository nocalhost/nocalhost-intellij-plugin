package dev.nocalhost.plugin.intellij.i18n;

import com.intellij.openapi.application.ApplicationManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Properties;

import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;

/**
 * Lightweight i18n helper for the Nocalhost plugin UI.
 *
 * <p>Languages are stored in plain {@code .properties} files under {@code /i18n/} so they can be
 * edited without touching Java code:
 * <ul>
 *   <li>{@code messages.properties}     - English (base)</li>
 *   <li>{@code messages_zh.properties}  - Chinese (zh)</li>
 * </ul>
 *
 * <p>The current language is read from {@link NocalhostSettings#getLanguage()} on every call and the
 * parsed tables are cached, so after the user changes the language in Settings
 * ({@code NocalhostI18n#refresh()} is invoked) every subsequent lookup instantly returns the new
 * language. Because context menus and dialogs create their actions/components each time they are
 * shown, translated strings take effect immediately without an IDE restart.
 *
 * <p>Only Nocalhost's own UI prompts are translated. Kubernetes resource terms (Deployment,
 * CronJob, Pods, Service, PVC, ...) are intentionally kept as-is and must not be added to the
 * properties files.
 */
public final class NocalhostI18n {

    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh";

    private static final String EN_RESOURCE = "/i18n/messages.properties";
    private static final String ZH_RESOURCE = "/i18n/messages_zh.properties";

    private static volatile Properties enCache;
    private static volatile Properties zhCache;

    private NocalhostI18n() {
    }

    public static boolean isZh() {
        return LANG_ZH.equals(getLanguage());
    }

    public static String getLanguage() {
        NocalhostSettings settings = ApplicationManager.getApplication().getService(NocalhostSettings.class);
        String language = settings.getLanguage();
        return language == null || language.isEmpty() ? LANG_EN : language;
    }

    public static String get(String key) {
        Properties props = isZh() ? zh() : en();
        String value = props.getProperty(key);
        if (value != null) {
            return value;
        }
        String enValue = en().getProperty(key);
        return enValue != null ? enValue : key;
    }

    public static String format(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    /** Clears the cached property tables. Call after the language setting changes. */
    public static void refresh() {
        enCache = null;
        zhCache = null;
    }

    private static Properties en() {
        Properties props = enCache;
        if (props == null) {
            props = load(EN_RESOURCE);
            enCache = props;
        }
        return props;
    }

    private static Properties zh() {
        Properties props = zhCache;
        if (props == null) {
            props = load(ZH_RESOURCE);
            zhCache = props;
        }
        return props;
    }

    private static Properties load(String resource) {
        Properties props = new Properties();
        try (InputStream in = NocalhostI18n.class.getResourceAsStream(resource)) {
            if (in != null) {
                // Read as UTF-8 so Chinese text can be written directly in the properties file.
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            // Missing/illegal resource falls back to the hard-coded English text.
        }
        return props;
    }
}
