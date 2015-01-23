package cucumber.runtime.arquillian.config;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class CucumberConfiguration {
    private static final CucumberConfiguration CONFIGURATION = new CucumberConfiguration();

    public static final String PERSISTENCE_EVENTS = "persistenceEventsActivated";
    public static final String COLORS = "colors";
    public static final String REPORTABLE = "reportable";
    public static final String REPORTABLE_PATH = "reportablePath";
    public static final String OPTIONS = "options";
    public static final String FEATURE_HOME = "featureHome";

    private boolean report;
    private boolean colorized;
    private boolean initialized;
    private String reportDirectory;
    private String options;
    private String featureHome;

    /**
     * directory to dump resource loader from loaders
     */
    private String tempDir = guessDefaultTempDir();

    private boolean persistenceEventsActivated = false;

    private static String guessDefaultTempDir() {
        final String suffix = "/cukespace/features/";
        if (new File("target").exists()) { // maven
            return new File("target", suffix).getAbsolutePath();
        }
        if (new File("build").exists()) {
            return new File("build", suffix).getAbsolutePath();
        }
        return System.getProperty("java.io.tmpdir") + suffix;
    }

    public CucumberConfiguration() {
        // no-op
    }

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

    public boolean hasOptions() {
        return options != null;
    }

    public String getOptions() {
        return options;
    }

    public String getFeatureHome() {
        return featureHome;
    }

    public boolean arePersistenceEventsActivated() {
        return persistenceEventsActivated;
    }

    public static File reportFile(final String path, final Class<?> clazz) {
        return new File(path, clazz.getName() + ".json");
    }

    public static CucumberConfiguration from(final Map<String, String> properties) {
        synchronized (CONFIGURATION) { // could it really be multithreaded?
            reset();

            if (properties.containsKey(PERSISTENCE_EVENTS)) {
                CONFIGURATION.persistenceEventsActivated = "true".equalsIgnoreCase(properties.get(PERSISTENCE_EVENTS));
            }
            if (properties.containsKey("tempDir")) {
                CONFIGURATION.tempDir = properties.get("tempDir");
            }
            if (properties.containsKey("report")) {
                CONFIGURATION.report = Boolean.parseBoolean(properties.get("report"));
            }
            if (properties.containsKey("reportDirectory")) {
                CONFIGURATION.reportDirectory = properties.get("reportDirectory");
            }
            if (properties.containsKey(COLORS)) {
                CONFIGURATION.colorized = Boolean.parseBoolean(properties.get(COLORS));
            }
            if (properties.containsKey(OPTIONS)) {
                CONFIGURATION.options = properties.get(OPTIONS);
            }
            if (properties.containsKey(FEATURE_HOME)) {
                CONFIGURATION.featureHome = properties.get(FEATURE_HOME);
            }

            CONFIGURATION.initialized = true;
        }
        return CONFIGURATION;
    }

    public static void reset() {
        CONFIGURATION.persistenceEventsActivated = false;
        CONFIGURATION.initialized = false;
        CONFIGURATION.reportDirectory = "target/cucumber-report/";
        CONFIGURATION.tempDir = guessDefaultTempDir();
        CONFIGURATION.options = null;
        CONFIGURATION.report = false;
        CONFIGURATION.colorized = !System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win")
                                        && !System.getProperty("java.class.path").contains("idea_rt");
    }

    public static CucumberConfiguration instance() {
        return CONFIGURATION;
    }

    public String getTempDir() {
        return tempDir;
    }
    
    public Properties getConfigurationAsProperties() {
    	final Properties configurationProperties = new Properties();
    	
    	if (this.isInitialized()) {    		
            configurationProperties.setProperty(CucumberConfiguration.PERSISTENCE_EVENTS, Boolean.toString(persistenceEventsActivated));
            configurationProperties.setProperty(CucumberConfiguration.COLORS, Boolean.toString(colorized));
            configurationProperties.setProperty(CucumberConfiguration.REPORTABLE, Boolean.toString(report));
            configurationProperties.setProperty(CucumberConfiguration.REPORTABLE_PATH, reportDirectory);
            if (featureHome != null) {
                configurationProperties.setProperty(CucumberConfiguration.FEATURE_HOME, featureHome);
            }
            if (this.hasOptions()) {
                configurationProperties.setProperty(CucumberConfiguration.OPTIONS, options);
            }
            if (featureHome != null) {
                configurationProperties.setProperty(CucumberConfiguration.FEATURE_HOME, featureHome);
            }
        }
    	
    	return configurationProperties;
    }
}
