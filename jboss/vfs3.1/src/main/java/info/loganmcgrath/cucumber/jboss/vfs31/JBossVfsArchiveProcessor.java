package info.loganmcgrath.cucumber.jboss.vfs31;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import cucumber.io.ResourceIteratorFactory;


/**
 * JBoss VFS archive processor.
 */
public class JBossVfsArchiveProcessor implements ApplicationArchiveProcessor
{
    /**
     * Initializes a new instance of the JBossVfsArchiveProcessor class.
     */
    public JBossVfsArchiveProcessor()
    {
        // intentionally empty
        
    } // JBossVfsArchiveProcessor
    
    
    @Override
    public void process( Archive<?> applicationArchive, TestClass testClass )
    {
        ( ( LibraryContainer<?> ) applicationArchive ).addAsLibrary(
            create( JavaArchive.class )
                .addAsServiceProvider( ResourceIteratorFactory.class, JBossVfsResourceIteratorFactory.class )
                .addClass( JBossVfsResource.class )
                .addClass( JBossVfsResourceIterator.class )
                .addClass( JBossVfsResourceIteratorFactory.class )
        );
        
    } // process
    
    
} // class JBossVfsArchiveProcessor
