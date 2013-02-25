package cucumber.runtime.arquillian.config;

import java.util.Locale;
import java.util.Properties;

public final class Configs {

    public static final String COLORS = "colors";

    private Configs() {
        // no-op
    }

    public static boolean areColorsAvailables() {
        return !System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win") // windows: no comment
                && !System.getProperty("java.class.path").contains("idea_rt"); // IDEA doesn't manage colors in its console
    }

    public static void initConfig(final Properties cukespaceConfig) {
        cukespaceConfig.setProperty(COLORS, Boolean.toString(areColorsAvailables()));
    }
}
