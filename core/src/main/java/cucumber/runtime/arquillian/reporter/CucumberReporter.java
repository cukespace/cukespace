package cucumber.runtime.arquillian.reporter;

import com.github.cukedoctor.Cukedoctor;
import com.github.cukedoctor.api.CukedoctorConverter;
import com.github.cukedoctor.api.DocumentAttributes;
import com.github.cukedoctor.api.model.Feature;
import com.github.cukedoctor.parser.FeatureParser;
import com.github.cukedoctor.util.FileUtil;
import cucumber.runtime.arquillian.config.CucumberConfiguration;
import net.masterthought.cucumber.ReportBuilder;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.jboss.arquillian.container.spi.event.KillContainer;
import org.jboss.arquillian.container.spi.event.StartContainer;
import org.jboss.arquillian.container.spi.event.StopContainer;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.Arrays.binarySearch;

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
                                false, false, false, false, true, false, true, false)
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

                    final DocumentAttributes da = bind(configuration.get().getConfig("adoc.doc.attributes."), new DocumentAttributes());
                    CukedoctorConverter converter = Cukedoctor.instance(features, da);
                    String doc = converter.renderDocumentation();
                    File adocFile = FileUtil.saveFile(configuration.get().getDocsDirectory() + "documentation.adoc", doc);

                    //TODO provide a way to user configure documentation
                    final OptionsBuilder optBuilder = OptionsBuilder.options()
                            .backend("html5")
                            .safe(SafeMode.UNSAFE);

                    final Map<String, String> opts = configuration.get().getConfig("adoc.options.");
                    if (!opts.isEmpty()) {
                        bind(opts, optBuilder);
                    }

                    final Map<String, String> attrs = configuration.get().getConfig("adoc.attributes.");
                    if (!attrs.isEmpty()) {
                        optBuilder.attributes(bind(attrs, AttributesBuilder.attributes()));
                    }

                    Map<String, Object> options = optBuilder
                            .asMap();
                    Asciidoctor asciidoctor = Asciidoctor.Factory.create();
                    //generate html(default backend) docs
                    asciidoctor.convertFile(adocFile, options);

                    //generate pdf docs
                    /**
                     * commented because of a classpath issue:
                     * java.lang.NoSuchMethodError: org.yaml.snakeyaml.events.DocumentStartEvent.getVersion()Lorg/yaml/snakeyaml/DumperOptions$Version;
                     */
                    //asciidoctor.convertFile(adocFile, OptionsBuilder.options().backend("pdf").safe(SafeMode.UNSAFE).asMap());

                    asciidoctor.shutdown();
                    LOGGER.info("Cucumber documentation generated at " + adocFile.getParent());
                }
            }
        }

        jsonReports.clear();
        CucumberConfiguration.reset();
    }

    private static <T> T bind(final Map<String, String> config, final T instance) {
        final Class<?>[] params = new Class<?>[] { String.class, boolean.class, File.class, Date.class, URI.class, int.class};

        for (final Map.Entry<String, String> entry : config.entrySet()) {
            final int dot = entry.getKey().lastIndexOf('.');
            final String key = entry.getKey().substring(dot + 1);

            boolean done = false;
            for (final String method : asList(key, "set" + Character.toLowerCase(key.charAt(0)) + key.substring(1))) {
                for (final Class<?> paramType : params) {
                    try {
                        final Method m = instance.getClass().getMethod(method, paramType);
                        final Object val;
                        if (paramType == boolean.class) {
                            val = Boolean.valueOf(entry.getValue());
                        } else if (paramType == int.class) {
                            val = Integer.valueOf(entry.getValue());
                        } else if (paramType == URI.class) {
                            val = new URI(entry.getValue());
                        } else if (paramType == Date.class) {
                            String pattern = config.get(key.substring(0, dot) + ".dateFormat");
                            if (pattern == null) {
                                pattern = "yyyy-MM-dd";
                            }
                            val = new SimpleDateFormat(pattern).parse(entry.getValue());
                        } else if (paramType == File.class) {
                            val = new File(entry.getValue());
                        } else {
                            val = entry.getValue();
                        }
                        m.invoke(instance, val);
                        done = true;
                    } catch (final Throwable th) { // NCDFE as well
                        // no-op
                    }
                }
                if (done) {
                    break;
                }
            }
            if (!done) {
                LOGGER.warning("Can't find matching property " + key + " in " + instance);
            }
        }
        return instance;
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
