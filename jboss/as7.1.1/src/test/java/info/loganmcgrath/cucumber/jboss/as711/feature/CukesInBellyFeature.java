package info.loganmcgrath.cucumber.jboss.as711.feature;

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
import info.loganmcgrath.cucumber.arquillian.domain.Belly;
import info.loganmcgrath.cucumber.arquillian.junit.Cucumber;
import info.loganmcgrath.cucumber.arquillian.junit.glue.BellySteps;


/**
 * Basic test for Cucumber tests in JBoss AS 7.1.1.Final.
 */
public class CukesInBellyFeature extends Cucumber
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
            .addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
            .addAsLibraries( use( MavenDependencyResolver.class )
                .loadMetadataFromPom( "pom.xml" )
                .goOffline()
                .artifact( "info.cukes:cucumber-java:jar" )
                .resolveAs( GenericArchive.class )
            )
            .addAsResource( "info/loganmcgrath/cucumber/arquillian/feature/cukes.feature" )
            .addClass( Belly.class )
            .addClass( BellySteps.class )
            .addClass( CukesInBellyFeature.class );
        
        return war;
        
    } // createDeployment
    
    
    /**
     * Initializes a new instance of the CukesInBellyFeature class.
     */
    public CukesInBellyFeature()
    {
        // intentionally empty
        
    } // CukesInBellyFeature
    
    
    /**
     * Initializes the runtime options.
     */
    @Before
    public void initializeRuntimeOptions()
    {
        RuntimeOptions runtimeOptions = this.getRuntimeOptions();
        
        runtimeOptions.featurePaths.add( "classpath:info/loganmcgrath/cucumber/arquillian/feature" );
        runtimeOptions.glue.add( "classpath:info/loganmcgrath/cucumber/arquillian/junit/glue" );
        
    } // initializeRuntimeOptions
    
    
} // class CukesInBellyFeature
