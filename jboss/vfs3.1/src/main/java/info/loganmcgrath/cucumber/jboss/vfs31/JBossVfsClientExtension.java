package info.loganmcgrath.cucumber.jboss.vfs31;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;


/**
 * JBoss VFS client extension.
 */
public class JBossVfsClientExtension implements LoadableExtension
{
    /**
     * Initializes a new instance of the JBossVfsClientExtension class.
     */
    public JBossVfsClientExtension()
    {
        // intentionally empty
        
    } // JBossVfsClientExtension
    
    
    @Override
    public void register( ExtensionBuilder builder )
    {
        builder.service( ApplicationArchiveProcessor.class, JBossVfsArchiveProcessor.class );
        
    } // register
    
    
} // class JBossVfsClientExtension
