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
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

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

        {//generate report using cukedoctor and asciidoctor
            if (configuration.get().getReportConfig() != null) {
                ReportConfig reportConfig = configuration.get().getReportConfig();
                List<Feature> features = new ArrayList<Feature>();
                for (String jsonReport : jsonReports) {
                    features.addAll(FeatureParser.parse(jsonReport));
                }
                if (features.isEmpty()) {
                    LOGGER.info("No features found for Cucumber documentation");
                } else {

                    final DocumentAttributes da = new DocumentAttributes();
                    CukedoctorConverter converter = Cukedoctor.instance(features, da);
                    String report = converter.renderDocumentation();
                    Asciidoctor asciidoctor = null;
                    for (String format : reportConfig.getFormats()) {
                        String destDir = reportConfig.getDirectory();
                        String fileName = reportConfig.getFileName();
                        final OptionsBuilder optBuilder = OptionsBuilder.options()
                                //.destinationDir(destDir)
                                .backend(format)
                                .safe(SafeMode.UNSAFE);


                        if(format.contains("html")){
                            if(reportConfig.hasHtmlConfig()){
                                optBuilder.attributes(reportConfig.getReportAttributes().getHtmlAtributes());
                                if(reportConfig.getHtmlAttribute("directory") != null){
                                    destDir = (String) reportConfig.getHtmlAttribute("directory");
                                    //optBuilder.destinationDir(destDir);
                                }
                                if(reportConfig.getHtmlAttribute("fileName") != null){
                                   fileName = (String) reportConfig.getHtmlAttribute("fileName");
                                }
                            }
                        }

                        if(format.contains("adoc")){
                            if(reportConfig.hasAdocConfig()){
                                optBuilder.attributes(reportConfig.getReportAttributes().getAdocAtributes());
                                if(reportConfig.getAdocAttribute("directory") != null){
                                    destDir = (String) reportConfig.getAdocAttribute("directory");
                                   // optBuilder.destinationDir(destDir);
                                }
                                if(reportConfig.getAdocAttribute("fileName") != null){
                                    fileName = (String) reportConfig.getAdocAttribute("fileName");
                                }
                            }
                        }

                        if(format.contains("pdf")){
                            if(reportConfig.hasPdfConfig()){
                                optBuilder.attributes(reportConfig.getReportAttributes().getPdfAtributes());
                                if(reportConfig.getPdfAttribute("directory") != null){
                                    destDir = (String) reportConfig.getPdfAttribute("directory");
                                    //optBuilder.destinationDir(destDir);
                                }
                                if(reportConfig.getPdfAttribute("fileName") != null){
                                    fileName = (String) reportConfig.getPdfAttribute("fileName");
                                }
                            }
                        }
                        File adocFile = FileUtil.saveFile(destDir + "/"+fileName + ".adoc", report);
                        if(!format.equals("adoc")){//there is no adoc backend
                            if(asciidoctor == null){
                                asciidoctor = Asciidoctor.Factory.create();
                            }
                            asciidoctor.convertFile(adocFile, optBuilder.asMap());
                            adocFile.deleteOnExit();
                        }
                        LOGGER.info("Cucumber report available at " + adocFile.getParent() + "/" + fileName + "." + format);
                    }

                    if(asciidoctor != null){
                        asciidoctor.shutdown();
                    }

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
