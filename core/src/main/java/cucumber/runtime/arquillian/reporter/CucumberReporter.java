package cucumber.runtime.arquillian.reporter;

import com.github.cukedoctor.Cukedoctor;
import com.github.cukedoctor.api.CukedoctorConverter;
import com.github.cukedoctor.api.model.Feature;
import com.github.cukedoctor.parser.FeatureParser;
import com.github.cukedoctor.util.FileUtil;
import cucumber.runtime.arquillian.config.CucumberConfiguration;
import net.masterthought.cucumber.ReportBuilder;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
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

        {//generate documentation using cukedoctor and asciidoctor
            if (configuration.get().isGenerateDocs()) {
                List<Feature> features = new ArrayList<Feature>();
                for (String jsonReport : jsonReports) {
                    features.addAll(FeatureParser.parse(jsonReport));
                }
                if (features.isEmpty()) {
                    LOGGER.info("No features found for Cucumber documentation");
                } else {

                    CukedoctorConverter converter = Cukedoctor.instance(features);
                    String doc = converter.renderDocumentation();
                    File adocFile = FileUtil.saveFile(configuration.get().getDocsDirectory() + "documentation.adoc", doc);
                    Asciidoctor asciidoctor = Asciidoctor.Factory.create();
                    //generate html(default backend) docs
                    asciidoctor.convertFile(adocFile, OptionsBuilder.options().backend(converter.getDocumentAttributes().getBackend()).safe(SafeMode.UNSAFE).asMap());

                    //generate pdf docs
                    /**
                     * commented because of a classpath issue:
                     * java.lang.NoSuchMethodError: org.yaml.snakeyaml.events.DocumentStartEvent.getVersion()Lorg/yaml/snakeyaml/DumperOptions$Version;
                     */
                    //asciidoctor.convertFile(adocFile, OptionsBuilder.options().backend("pdf").safe(SafeMode.UNSAFE).asMap());

                    asciidoctor.shutdown();
                    LOGGER.info("Cucumber documentation generated at " +adocFile.getParent());
                }
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
