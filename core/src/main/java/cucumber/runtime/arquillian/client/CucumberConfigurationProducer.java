package cucumber.runtime.arquillian.client;

import cucumber.runtime.arquillian.config.CucumberConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.util.Map;

public class CucumberConfigurationProducer {
    @Inject @ApplicationScoped
    private InstanceProducer<CucumberConfiguration> configurationProducer;

    public void findConfiguration(final @Observes ArquillianDescriptor descriptor) {
        final ExtensionDef cucumberDef = descriptor.extension("cucumber");
        CucumberConfiguration config = CucumberConfiguration.from(cucumberDef.getExtensionProperties());
        configurationProducer.set(config);
        ExtensionDef adocExtension = descriptor.extension("cucumber-report-adoc");
        if(adocExtension != null) {
            configAdocExtension(adocExtension, config);
        }
    }

    private void configAdocExtension(ExtensionDef adocExtension, CucumberConfiguration config) {
            Map<String, String> adocReportConfig = config.getConfig("adoc.doc.attributes");

            for (String s : adocExtension.getExtensionProperties().keySet()) {
                /**
                 <property name="attributes">
                 linkcss = true,
                 icons = font
                 </property>
                 */
                if(s.equals("attributes")) {
                    String[] attrs = adocExtension.getExtensionProperties().get(s).replaceAll("\\s", "").split(",");
                    for (String attr : attrs) {
                        if (!attr.contains("=")) {
                            continue;
                        }
                        adocReportConfig.put("adoc.doc.attributes." + attr.split("=")[0], attr.split("=")[1]);
                    }
                }
            }
        config.putConfig(adocReportConfig);
    }
}
