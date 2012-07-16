package info.loganmcgrath.cucumber.arquillian.io;

import java.util.Iterator;

import cucumber.io.Resource;


/**
 * Arquillian classpath iterable.
 */
public class ArquillianClasspathIterable implements Iterable<Resource>
{
    /**
     * Initializes a new instance of the ArquillianClasspathIterable class.
     * 
     * @param classLoader The class loader to use. 
     * @param path The classpath.
     * @param suffix The suffix used to filter to resource names.
     */
    public ArquillianClasspathIterable( ClassLoader classLoader, String path, String suffix )
    {
        // TODO Auto-generated constructor stub
        
    } // ArquillianClasspathIterable
    
    
    @Override
    public Iterator<Resource> iterator()
    {
        // TODO Auto-generated method stub
        return null;
        
    } // iterator
    
    
} // class ArquillianClasspathIterable
