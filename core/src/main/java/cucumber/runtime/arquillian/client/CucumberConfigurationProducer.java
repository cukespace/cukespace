package cucumber.runtime.arquillian.client;

import cucumber.runtime.arquillian.config.CucumberConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.config.descriptor.impl.ArquillianDescriptorImpl;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.shrinkwrap.descriptor.spi.node.Node;

public class CucumberConfigurationProducer {
    @Inject @ApplicationScoped
    private InstanceProducer<CucumberConfiguration> configurationProducer;

    public void findConfiguration(final @Observes ArquillianDescriptor descriptor) {
        final ExtensionDef cucumberDef = descriptor.extension("cucumber");
        CucumberConfiguration cucumberConfiguration = CucumberConfiguration.from(cucumberDef.getExtensionProperties());
        configurationProducer.set(cucumberConfiguration);
        if(!hasAsciidoctorExtension(descriptor) && cucumberConfiguration.isGenerateDocs()) {
            autoConfigureAsciidoctorExtension(descriptor);
        }
    }

    private void autoConfigureAsciidoctorExtension(ArquillianDescriptor descriptor) {
        ArquillianDescriptorImpl descriptorImpl = (ArquillianDescriptorImpl) descriptor;
        Node extension = descriptorImpl.getRootNode().createChild("extension");
        extension.attribute("qualifier", "asciidoctor-docs");
        extension.createChild("property")
                .attribute("name", "sourceDirectory").text("target/docs");
        extension.createChild("property")
                .attribute("name", "outputDirectory").text("target/docs");
        extension.createChild("property")
                .attribute("name", "backend").text("html");
        extension.createChild("property")
                .attribute("name", "attribute.icons").text("font");
        extension.createChild("property")
                .attribute("name", "attribute.toc").text("right");
    }


    private boolean hasAsciidoctorExtension(ArquillianDescriptor descriptor) {
        if(descriptor.getExtensions() == null || descriptor.getExtensions().isEmpty()) {
            return false;
        }

        for (ExtensionDef extension : descriptor.getExtensions()) {
            if(extension.getExtensionName().startsWith("asciidoctor")) {
                return true;
            }
        }
        return false;
    }
}
