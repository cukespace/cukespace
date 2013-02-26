package cucumber.runtime.arquillian.reporter;

import net.masterthought.cucumber.ReportBuilder;
import org.jboss.arquillian.container.spi.event.KillContainer;
import org.jboss.arquillian.container.spi.event.StartContainer;
import org.jboss.arquillian.container.spi.event.StopContainer;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CucumberReporter {
    private static Set<String> jsonReports;

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

        {
            // todo
            final File outputDir = new File("target/cucumber-report-html/");
            final String buildNumber = "1";
            final String projectName = "Cucumber test";

            try {
                final ReportBuilder builder = new ReportBuilder(new ArrayList<String>(jsonReports),
                                                                outputDir, "/", buildNumber, projectName,
                                                                false, false, true, false);
                builder.generateReports();
            } catch (final Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        jsonReports.clear();
    }
}
