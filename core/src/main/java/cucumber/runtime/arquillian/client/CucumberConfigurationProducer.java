package cucumber.runtime.arquillian.client;

import cucumber.runtime.arquillian.config.CucumberConfiguration;
import cucumber.runtime.arquillian.reporter.ReportAttributes;
import cucumber.runtime.arquillian.reporter.ReportConfig;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.util.Map;

public class CucumberConfigurationProducer {
    @Inject
    @ApplicationScoped
    private InstanceProducer<CucumberConfiguration> configurationProducer;

    public void findConfiguration(final @Observes ArquillianDescriptor descriptor) {
        final ExtensionDef cucumberDef = descriptor.extension("cucumber");
        CucumberConfiguration config = CucumberConfiguration.from(cucumberDef.getExtensionProperties());
        configurationProducer.set(config);
        ExtensionDef reportExtension = descriptor.extension("cucumber-report");
        if (reportExtension != null) {
            configReportExtension(reportExtension, config);
        }
    }

    private void configReportExtension(ExtensionDef reportExtension, CucumberConfiguration config) {
        ReportConfig reportConfig = new ReportConfig();
        Map<String, String> props = reportExtension.getExtensionProperties();
        String dir = props.get("directory");
        if (dir != null) {
            reportConfig.setDirectory(dir);
        }

        String formats = props.get("formats");
        if (formats != null) {
            reportConfig.setFormats(formats.split(","));
        }
        String attrs = props.get("attributes");
        if(attrs != null){
            if(attrs.contains("\t")){
                attrs = attrs.replaceAll("\t","");
            }
            Yaml yaml = new Yaml(new Constructor(ReportAttributes.class));
            ReportAttributes reportAttributes = (ReportAttributes) yaml.load(attrs);
            reportConfig.setReportAttributes(reportAttributes);
        }

        config.setReportConfig(reportConfig);

    }
}
