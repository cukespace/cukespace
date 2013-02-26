package cucumber.runtime.arquillian.config;

import java.io.File;

public final class Configs {

    public static final String COLORS = "colors";
    public static final String REPORTABLE = "reportable";
    public static final String REPORTABLE_PATH = "reportablePath";

    private Configs() {
        // no-op
    }

    public static File reportFile(final String path, final Class<?> clazz) {
        return new File(path, clazz.getName() + ".json");
    }
}
