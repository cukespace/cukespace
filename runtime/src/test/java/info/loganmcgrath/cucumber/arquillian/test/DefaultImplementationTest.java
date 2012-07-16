package info.loganmcgrath.cucumber.arquillian.test;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.jboss.shrinkwrap.resolver.api.DependencyResolvers.use;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Test;
import org.junit.runner.RunWith;

import cucumber.io.MultiLoader;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import info.loganmcgrath.cucumber.arquillian.domain.Belly;
import info.loganmcgrath.cucumber.arquillian.steps.BellySteps;


/**
 * Test to show-case shortcomings of "default" cucumber implementation in
 * container environment.
 */
@RunWith( Arquillian.class )
public class DefaultImplementationTest
{
    /**
     * Creates the test deployment.
     * 
     * @return The test deployment.
     */
    @Deployment
    public static Archive<?> createDeployment()
    {
        WebArchive war = create( WebArchive.class )
            .addAsLibraries( use( MavenDependencyResolver.class )
                .loadMetadataFromPom( "pom.xml" )
                .artifact( "info.cukes:cucumber-core:jar" )
                .artifact( "info.cukes:cucumber-java:jar" )
                .resolveAs( GenericArchive.class ) );
        
        war.addClasses(
            DefaultImplementationTest.class,
            Belly.class,
            BellySteps.class
        );
        
        return war;
        
    } // createDeployment
    
    
    /**
     * Initializes a new instance of the DefaultImplementationTest class.
     */
    public DefaultImplementationTest()
    {
        // intentionally empty
        
    } // DefaultImplementationTest
    
    
    /**
     * Shows that multi-loader fails to find classpath resources in container
     * environment.
     * 
     * <p>Due to this limitation, a different ResourceLoader implementation is
     * required.</p>
     * 
     * @throws Exception Thrown if something effs up.
     */
    @Test( expected = IllegalArgumentException.class )
    public void shouldFail() throws Exception
    {
        RuntimeOptions runtimeOptions = new RuntimeOptions( System.getProperties() );
        ClassLoader classLoader = this.getClass().getClassLoader();
        
        new Runtime( new MultiLoader( classLoader ), classLoader, runtimeOptions );
        
    } // shouldFail
    
    
} // class DefaultImplementationTest
