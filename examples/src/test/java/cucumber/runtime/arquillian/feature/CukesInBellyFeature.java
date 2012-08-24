package cucumber.runtime.arquillian.feature;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.arquillian.domain.Belly;
import cucumber.runtime.arquillian.glue.server.BellySteps;
import cucumber.runtime.arquillian.junit.Cucumber;

/**
 * Basic test for Cucumber features in JBoss AS 7.
 */
public class CukesInBellyFeature extends Cucumber {
    
    /**
     * Creates the test deployment.
     * 
     * @return The test deployment.
     */
    @Deployment
    public static Archive<?> createDeployment() {
        WebArchive war = create(WebArchive.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsResource("cucumber/runtime/arquillian/feature/cukes.feature")
            .addClass(Belly.class)
            .addClass(BellySteps.class)
            .addClass(CukesInBellyFeature.class);
        
        return war;
    }
    
    /**
     * Initializes a new instance of the CukesInBellyFeature class.
     */
    public CukesInBellyFeature() {
        // intentionally empty
    }
    
    @Override
    protected void initializeRuntimeOptions() {
        RuntimeOptions runtimeOptions = this.getRuntimeOptions();
        runtimeOptions.featurePaths.add("classpath:cucumber/runtime/arquillian/feature");
        runtimeOptions.glue.add("classpath:cucumber/runtime/arquillian/glue/server");
    }
}
