package info.loganmcgrath.cucumber.arquillian.client;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;


/**
 * Cucumber client extension.
 */
public class CucumberClientExtension implements LoadableExtension
{
    /**
     * Initializes a new instance of the CucumberClientExtension class.
     */
    public CucumberClientExtension()
    {
        // intentionally empty
        
    } // CucumberClientExtension
    
    
    @Override
    public void register( ExtensionBuilder builder )
    {
        builder.service( ApplicationArchiveProcessor.class, CucumberArchiveProcessor.class );
        
    } // register
    
    
} // class CucumberClientExtension
