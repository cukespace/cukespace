package info.loganmcgrath.cucumber.arquillian.io;

import cucumber.io.ClasspathResourceLoader;
import cucumber.io.Resource;


/**
 * Arquillian resource loader.
 */
public class ArquillianResourceLoader extends ClasspathResourceLoader
{
    /**
     * The class loader to use.
     */
    private ClassLoader classLoader;
    
    
    /**
     * Initializes a new instance of the ArquillianResourceLoader class.
     * 
     * @param classLoader The class loader to use.
     */
    public ArquillianResourceLoader( ClassLoader classLoader )
    {
        super( classLoader );
        
        this.classLoader = classLoader;
        
    } // ArquillianResourceLoader
    
    
    @Override
    public Iterable<Resource> resources( String path, String suffix )
    {
        return new ArquillianClasspathIterable( this.classLoader, path, suffix );
        
    } // resources
    
    
} // class ArquillianResourceLoader
