package info.loganmcgrath.cucumber.arquillian.feature.jbossas;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.jboss.shrinkwrap.resolver.api.DependencyResolvers.use;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Test;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.runner.RunWith;

import cucumber.io.MultiLoader;
import cucumber.io.ResourceIteratorFactory;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import info.loganmcgrath.cucumber.arquillian.domain.Belly;
import info.loganmcgrath.cucumber.arquillian.steps.BellySteps;
import info.loganmcgrath.cucumber.jbossvfs.io.JBossVfsResource;
import info.loganmcgrath.cucumber.jbossvfs.io.JBossVfsResourceIterator;
import info.loganmcgrath.cucumber.jbossvfs.io.JBossVfsResourceIteratorFactory;


/**
 * Proof of concept for JBoss VFS resource iterator.
 */
@RunWith( Arquillian.class )
public class CukesInBellyFeature
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
                .goOffline()
                .artifact( "info.cukes:cucumber-core:jar" )
                .artifact( "info.cukes:cucumber-java:jar" )
                .resolveAs( GenericArchive.class )
            );
        
        war.addAsResource( "info/loganmcgrath/cucumber/arquillian/feature/cukes.feature" );
        
        war.addClasses(
            CukesInBellyFeature.class,
            Belly.class,
            BellySteps.class,
            JBossVfsResource.class,
            JBossVfsResourceIterator.class,
            JBossVfsResourceIteratorFactory.class
        );
        
        war.addAsServiceProvider( ResourceIteratorFactory.class, JBossVfsResourceIteratorFactory.class );
        
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
     * Run the feature and see what happens!
     * 
     * @throws Exception Thrown if something effs up.
     */
    @Test
    public void shouldNotFail() throws Exception
    {
        RuntimeOptions runtimeOptions = new RuntimeOptions( System.getProperties() );
        runtimeOptions.featurePaths.add( "classpath:info/loganmcgrath/cucumber/arquillian/feature" );
        runtimeOptions.glue.add( "classpath:info/loganmcgrath/cucumber/arquillian/steps" );
        
        ClassLoader classLoader = this.getClass().getClassLoader();
        Runtime runtime = new Runtime( new MultiLoader( classLoader ), classLoader, runtimeOptions );
        
        runtime.run();
        
        List<Throwable> errors = runtime.getErrors();
        
        if( !errors.isEmpty() )
        {
            throw new MultipleFailureException( runtime.getErrors() );
        }
        
    } // shouldNotFail
    
    
} // class CukesInBellyFeature
