package cucumber.runtime.arquillian.jbas7;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.vfs.VirtualFile;

import cucumber.io.Resource;

public class Jbas7Resource implements Resource {
    
    private final String name;
    
    private final VirtualFile virtualFile;
    
    public Jbas7Resource(VirtualFile virtualFile, String name) {
        this.name = name;
        this.virtualFile = virtualFile;
    }
    
    @Override
    public String getClassName() {
        String path = this.getPath();
        
        if (path.endsWith(".class")) {
            return path.substring(0, path.length() - 6).replace('/', '.');
        }
        
        throw new IllegalArgumentException("Resource is not a class file: " + path);
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
