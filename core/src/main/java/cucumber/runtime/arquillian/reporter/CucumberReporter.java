package cucumber.runtime.arquillian.reporter;

import cucumber.runtime.arquillian.config.CucumberConfiguration;
import net.masterthought.cucumber.ReportBuilder;
import org.jboss.arquillian.container.spi.event.KillContainer;
import org.jboss.arquillian.container.spi.event.StartContainer;
import org.jboss.arquillian.container.spi.event.StopContainer;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class CucumberReporter {
    private static final Logger LOGGER = Logger.getLogger(CucumberReporter.class.getName());

    private static Set<String> jsonReports;

    @Inject
    private Instance<CucumberConfiguration> configuration;

    public void initOnStart(final @Observes StartContainer startContainer) {
        jsonReports = new HashSet<String>();
    }

    public void reportOnStop(final @Observes StopContainer stopContainer) {
        doReport();
    }

    public void reportOnKill(final @Observes KillContainer stopContainer) {
        doReport();
    }

    public static void addReport(final File path) {
        if (jsonReports != null) {
            jsonReports.add(path.getAbsolutePath());
        }
    }

    private void doReport() {
        if (jsonReports == null || jsonReports.isEmpty()) {
            return;
        }

        { // remove not existing files
            final Iterator<String> files = jsonReports.iterator();
            while (files.hasNext()) {
                if (!new File(files.next()).exists()) {
                    files.remove();
                }
            }
        }

        { // generate the report
            final File outputDir = new File(configuration.get().getReportDirectory());
            try {
                new ReportBuilder(new ArrayList<String>(jsonReports),
                                outputDir, "/", "#", findProjectName(),
                                false, false, false, false, true, false, false, "", true, false)
                        .generateReports();

                LOGGER.info("Cucumber report available at "
                        + new File(outputDir, "feature-overview.html").getAbsolutePath());
            } catch (final Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        jsonReports.clear();
        CucumberConfiguration.reset();
    }

    private static String findProjectName() {
        File file = new File(".");
        while (file != null) {
            if (Arrays.asList("target", "classes").contains(file.getName())) {
                file = file.getParentFile();
            } else {
                return file.getName();
            }
        }
        return "Cucumber Report";
    }
}
