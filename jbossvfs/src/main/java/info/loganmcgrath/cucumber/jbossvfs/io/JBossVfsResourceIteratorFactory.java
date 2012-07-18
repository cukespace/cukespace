package info.loganmcgrath.cucumber.jbossvfs.io;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.jboss.vfs.VirtualFile;

import cucumber.io.Resource;
import cucumber.io.ResourceIteratorFactory;
import cucumber.runtime.CucumberException;


/**
 * Resource iterator factory for resources found in the JBoss VFS.
 */
public class JBossVfsResourceIteratorFactory implements ResourceIteratorFactory
{
    /**
     * Initializes a new instance of the JBossVfsResourceIteratorFactory class.
     */
    public JBossVfsResourceIteratorFactory()
    {
        // intentionally empty
        
    } // JBossVfsResourceIteratorFactory
    
    
    @Override
    public boolean isFactoryFor( URL url )
    {
        return "vfs".equals( url.getProtocol() );
        
    } // isFactoryFor
    
    
    @Override
    public Iterator<Resource> createIterator( URL url, String path, String suffix )
    {
        try
        {
            Object content = url.openConnection().getContent();
            
            if( content instanceof VirtualFile )
            {
                return new JBossVfsResourceIterator( ( VirtualFile ) content, path, suffix );
            }
            else
            {
                throw new IllegalArgumentException( new StringBuilder()
                    .append( "URL " )
                    .append( url )
                    .append( " does not refer to valid JBoss VFS resource" )
                    .toString() );
            }
        }
        catch( IOException exception )
        {
            throw new CucumberException( exception );
        }
        
    } // createIterator
    
    
} // class JBossVfsResourceIteratorFactory
