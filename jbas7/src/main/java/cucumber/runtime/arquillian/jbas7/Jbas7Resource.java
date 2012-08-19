package cucumber.runtime.arquillian.jbas7;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.vfs.VirtualFile;

import cucumber.io.Resource;

/**
 * JBoss AS 7 virtual filesystem resource.
 */
public class Jbas7Resource implements Resource {
    
    /**
     * The name of the virtual resource.
     */
    private final String name;
    
    /**
     * The virtual resource.
     */
    private final VirtualFile virtualFile;
    
    /**
     * Initializes a new instance of the Jbas7Resource class.
     * 
     * @param virtualFile The virtual resource.
     * @param name The name of the virtual resource relative to the classpath.
     */
    public Jbas7Resource(VirtualFile virtualFile, String name) {
        this.name = name;
        this.virtualFile = virtualFile;
    }
    
    @Override
    public String getClassName() {
        String path = this.getPath();
        
        return path.substring(0, path.length() - 6).replace('/', '.');
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return this.virtualFile.openStream();
    }
    
    @Override
    public String getPath() {
        return this.name;
    }
}
