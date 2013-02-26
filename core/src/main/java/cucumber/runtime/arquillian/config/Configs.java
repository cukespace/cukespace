package cucumber.runtime.arquillian.config;

import cucumber.runtime.arquillian.api.Reportable;

import java.io.File;
import java.util.Locale;
import java.util.Properties;

public final class Configs {

    public static final String COLORS = "colors";
    public static final String REPORTABLE = "reportable";
    public static final String REPORTABLE_PATH = "reportablePath";

    private Configs() {
        // no-op
    }

    public static boolean areColorsAvailables() {
        return !System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win") // windows: no comment
                && !System.getProperty("java.class.path").contains("idea_rt"); // IDEA doesn't manage colors in its console
    }

    public static void initConfig(final Class<?> clazz, final Properties cukespaceConfig) {
        final Reportable reportable = clazz.getAnnotation(Reportable.class);

        cukespaceConfig.setProperty(COLORS, Boolean.toString(areColorsAvailables()));
        cukespaceConfig.setProperty(REPORTABLE, Boolean.toString(reportable != null));
        cukespaceConfig.setProperty(REPORTABLE_PATH, reportablePath(reportable));
    }

    public static String reportablePath(final Reportable annotation) {
        if (annotation == null) {
            return "no";
        }
        return annotation.value().replace("$pwd", new File(".").getAbsolutePath());
    }

    public static File reportFile(final String path, final Class<?> clazz) {
        return new File(path, clazz.getName() + ".json");
    }
}
