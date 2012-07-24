package info.loganmcgrath.cucumber.jboss.vfs31;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jboss.vfs.VirtualFile;

import cucumber.io.Resource;
import cucumber.runtime.CucumberException;


/**
 * JBoss VFS resource iterator.
 */
public class JBossVfsResourceIterator implements Iterator<Resource>
{
    /**
     * The child elements in the virtual resource.
     */
    private final Iterator<VirtualFile> elements;
    
    /**
     * The next applicable resource.
     */
    private Resource next;
    
    /**
     * The path.
     */
    private final String path;
    
    /**
     * The suffix.
     */
    private final String suffix;
    
    
    /**
     * Initializes a new instance of the JBossVfsResourceIterator class.
     * 
     * @param virtualFile The virtual file.
     * @param path The path.
     * @param suffix The suffix.
     */
    public JBossVfsResourceIterator( VirtualFile virtualFile, String path, String suffix )
    {
        try
        {
            this.elements = virtualFile.getChildrenRecursively().iterator();
        }
        catch( IOException exception )
        {
            throw new CucumberException( exception );
        }
        
        this.path = path;
        this.suffix = suffix;
        
        this.moveNext();
        
    } // JBossVfsResourceIterator
    
    
    @Override
    public boolean hasNext()
    {
        return this.next != null;
        
    } // hasNext
    
    
    @Override
    public Resource next()
    {
        try
        {
            if( this.hasNext() )
            {
                return this.next;
            }
            else
            {
                throw new NoSuchElementException();
            }
        }
        finally
        {
            this.moveNext();
        }
        
    } // next
    
    
    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
        
    } // remove
    
    
    /**
     * Gets the virtual file's path relative to the classpath.
     * 
     * @param virtualFile The virtual file.
     * @return The file path relative to the classpath.
     */
    private String getPathRelativeToClasspath( VirtualFile virtualFile )
    {
        return virtualFile.getPathName().replaceFirst( "^.+(\\.jar|\\.war/WEB-INF/classes)/", "" );
        
    } // stripVfsPath
    
    
    /**
     * Checks if a resource path has a the specified suffix.
     * 
     * @param path The path to check.
     * @return True, if the path has the suffix.
     */
    private boolean hasSuffix( String path )
    {
        return this.suffix == null
            || "".equals( this.suffix )
            || path.endsWith( this.suffix );
        
    } // hasSuffix
    
    
    /**
     * Moves to the next child in the resource.
     */
    private void moveNext()
    {
        this.next = null;
        
        while( this.elements.hasNext() )
        {
            VirtualFile virtualFile = this.elements.next();
            String name = this.getPathRelativeToClasspath( virtualFile );
            
            if( name.startsWith( this.path ) && this.hasSuffix( name ) )
            {
                this.next = new JBossVfsResource( virtualFile, name );
                
                break;
            }
        }
        
    } // moveNext
    
    
} // class JBossVfsResourceIterator
