package cucumber.runtime.arquillian.config;

import java.io.File;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

public class CucumberConfiguration implements Serializable {
    private static final CucumberConfiguration CONFIGURATION = new CucumberConfiguration();

    public static final String COLORS = "colors";
    public static final String REPORTABLE = "reportable";
    public static final String REPORTABLE_PATH = "reportablePath";

    private boolean report = false;
    private boolean colorized = !System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win")
                                    && !System.getProperty("java.class.path").contains("idea_rt");
    private boolean initialized = false;
    private String reportDirectory = "target/cucumber-report/";

    public boolean isReport() {
        return report;
    }

    public String getReportDirectory() {
        return reportDirectory;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isColorized() {
        return colorized;
    }

    public static File reportFile(final String path, final Class<?> clazz) {
        return new File(path, clazz.getName() + ".json");
    }

    public static CucumberConfiguration from(final Map<String, String> properties) {
        synchronized (CONFIGURATION) { // could it really be multithreaded?
            if (properties.containsKey("report")) {
                CONFIGURATION.report = Boolean.parseBoolean(properties.get("report"));
            }
            if (properties.containsKey("reportDirectory")) {
                CONFIGURATION.reportDirectory = properties.get("reportDirectory");
            }
            if (properties.containsKey("reportDirectory")) {
                CONFIGURATION.reportDirectory = properties.get("reportDirectory");
            }
            if (properties.containsKey("colors")) {
                CONFIGURATION.colorized = Boolean.parseBoolean(properties.get("colors"));
            }

            CONFIGURATION.initialized = true;
        }
        return CONFIGURATION;
    }

    public static void reset() {
        CONFIGURATION.initialized = false;
    }

    public static CucumberConfiguration instance() {
        return CONFIGURATION;
    }
}
