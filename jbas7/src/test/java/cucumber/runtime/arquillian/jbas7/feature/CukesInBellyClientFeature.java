package cucumber.runtime.arquillian.jbas7.feature;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.arquillian.controller.BellyController;
import cucumber.runtime.arquillian.domain.Belly;
import cucumber.runtime.arquillian.junit.CucumberClient;
import cucumber.runtime.arquillian.producer.FacesContextProducer;

/**
 * Cucumber feature run from the client.
 */
public class CukesInBellyClientFeature extends CucumberClient {
    
    /**
     * Creates the test deployment.
     * 
     * @return The test deployment.
     */
    @Deployment(testable = false)
    public static Archive<?> createDeployment() {
        Class<?> klass = CukesInBellyClientFeature.class;
        return create(WebArchive.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource(new StringAsset("<faces-config version=\"2.0\"/>"), "faces-config.xml")
            .addAsWebResource(klass.getResource("/webapp/belly.xhtml"), "belly.xhtml")
            .addClass(Belly.class)
            .addClass(BellyController.class)
            .addClass(FacesContextProducer.class);
    }
    
    /**
     * Initializes a new instance of the CukesInBellyClientFeature class.
     */
    public CukesInBellyClientFeature() {
        // intentionally empty
    }
    
    @Override
    protected void initializeRuntimeOptions() {
        RuntimeOptions runtimeOptions = this.getRuntimeOptions();
        runtimeOptions.featurePaths.add("classpath:cucumber/runtime/arquillian/feature");
        runtimeOptions.glue.add("classpath:cucumber/runtime/arquillian/glue/ui");
    }
}
