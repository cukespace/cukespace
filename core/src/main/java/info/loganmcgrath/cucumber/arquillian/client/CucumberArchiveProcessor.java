package info.loganmcgrath.cucumber.arquillian.client;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.jboss.shrinkwrap.resolver.api.DependencyResolvers.use;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import cucumber.runtime.arquillian.TestEnricherProvider;

import info.loganmcgrath.cucumber.arquillian.container.CucumberContainerExtension;


/**
 * Archive processor for Cucumber features.
 */
public class CucumberArchiveProcessor implements ApplicationArchiveProcessor
{
    /**
     * Initializes a new instance of the CucumberArchiveProcessor class.
     */
    public CucumberArchiveProcessor()
    {
        // intentionally empty
        
    } // CucumberArchiveProcessor
    
    
    @Override
    public void process( Archive<?> applicationArchive, TestClass testClass )
    {
        Properties properties = new Properties();
        InputStream versionsStream = CucumberArchiveProcessor.class.getResourceAsStream( "versions.properties" );
        
        try
        {
            properties.load( versionsStream );
        }
        catch( IOException exception )
        {
            throw new RuntimeException( exception );
        }
        finally
        {
            try
            {
                versionsStream.close();
            }
            catch( IOException exception )
            {
                // intentionally empty
            }
        }
        
        LibraryContainer<?> libraryContainer = ( LibraryContainer<?> ) applicationArchive;
        
        libraryContainer.addAsLibraries(
            use( MavenDependencyResolver.class )
                .artifact( "info.cukes:cucumber-java:" + properties.getProperty( "cucumber-jvm.version" ) )
                .resolveAs( GenericArchive.class )
        );
        
        libraryContainer.addAsLibrary(
            create( JavaArchive.class )
                .addAsServiceProvider( RemoteLoadableExtension.class, CucumberContainerExtension.class )
                .addClass( CucumberContainerExtension.class )
                .addClass( TestEnricherProvider.class )
        );
        
    } // process
    
    
} // class CucumberArchiveProcessor
