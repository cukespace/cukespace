package info.loganmcgrath.cucumber.jboss.vfs31;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.vfs.VirtualFile;

import cucumber.io.Resource;


/**
 * JBoss VFS resource.
 */
public class JBossVfsResource implements Resource
{
    /**
     * The name of the virtual resource.
     */
    private final String name;
    
    /**
     * The virtual resource.
     */
    private final VirtualFile virtualFile;
    
    
    /**
     * Initializes a new instance of the JBossVfsResource class.
     * 
     * @param virtualFile The virtual resource.
     * @param name The name of the virtual resource relative to the classpath.
     */
    public JBossVfsResource( VirtualFile virtualFile, String name )
    {
        this.name = name;
        this.virtualFile = virtualFile;
        
    } // JBossVfsResource
    
    
    @Override
    public String getClassName()
    {
        String path = this.getPath();
        
        return path.substring( 0, path.length() - 6 ).replace( '/', '.' );
        
    } // getClassName
    
    
    @Override
    public InputStream getInputStream() throws IOException
    {
        return this.virtualFile.openStream();
        
    } // getInputStream
    
    
    @Override
    public String getPath()
    {
        return this.name;
        
    } // getPath
    
    
} // class JBossVfsResource
