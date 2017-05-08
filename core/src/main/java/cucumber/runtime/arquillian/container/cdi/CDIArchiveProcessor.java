package cucumber.runtime.arquillian.container.cdi;

import cucumber.runtime.arquillian.client.CucumberArchiveProcessor;
import cucumber.runtime.arquillian.config.CucumberConfiguration;
import cucumber.runtime.arquillian.container.CukeSpaceCDIObjectFactory;
import java.util.Map;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class CDIArchiveProcessor extends CucumberArchiveProcessor {

    @Inject
    private Instance<CucumberConfiguration> configuration;

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        super.process(applicationArchive, testClass);

        CucumberConfiguration cucumberConfiguration = configuration.get();
        Archive<?> entryPointContainer = findArchiveByTestClass(applicationArchive, testClass.getJavaClass());

        boolean cdiEnabled = CukeSpaceCDIObjectFactory.class.getCanonicalName()
                .equalsIgnoreCase(cucumberConfiguration.getObjectFactory().trim());

        if (cdiEnabled) {
            enrichWithCDI(entryPointContainer);
        }
    }

    private void enrichWithCDI(Archive<?> applicationArchive) {
        Map<ArchivePath, Node> contentMap = applicationArchive.getContent(Filters.include(".*/cukespace-core.jar"));
        for (Node node : contentMap.values()) {
            if (node.getAsset() instanceof ArchiveAsset) {
                JavaArchive archive = (JavaArchive) ((ArchiveAsset) node.getAsset()).getArchive();

                archive.addClass(CukeSpaceCDIObjectFactory.class);
                archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
            }
        }
    }
}
