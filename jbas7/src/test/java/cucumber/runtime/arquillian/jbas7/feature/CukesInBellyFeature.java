package cucumber.runtime.arquillian.jbas7.feature;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.jboss.shrinkwrap.resolver.api.DependencyResolvers.use;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Before;

import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.arquillian.domain.Belly;
import cucumber.runtime.arquillian.glue.BellySteps;
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
            .addAsLibraries(use(MavenDependencyResolver.class)
                .loadMetadataFromPom("pom.xml")
                .goOffline()
                .artifact("info.cukes:cucumber-java:jar")
                .resolveAs(GenericArchive.class)
            )
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
    
    /**
     * Initializes the runtime options.
     */
    @Before
    public void initializeRuntimeOptions() {
        RuntimeOptions runtimeOptions = this.getRuntimeOptions();
        runtimeOptions.featurePaths.add("classpath:cucumber/runtime/arquillian/feature" );
        runtimeOptions.glue.add("classpath:cucumber/runtime/arquillian/glue" );
    }
}
