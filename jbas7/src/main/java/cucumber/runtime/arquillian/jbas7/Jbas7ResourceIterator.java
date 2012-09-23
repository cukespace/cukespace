package cucumber.runtime.arquillian.jbas7;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jboss.vfs.VirtualFile;

import cucumber.io.Resource;
import cucumber.runtime.CucumberException;

public class Jbas7ResourceIterator implements Iterator<Resource> {
    
    private final Iterator<VirtualFile> elements;
    
    private Resource next;
    
    private final String path;
    
    private final String suffix;
    
    public Jbas7ResourceIterator(VirtualFile virtualFile, String path, String suffix) {
        try {
            this.elements = virtualFile.getChildrenRecursively().iterator();
        } catch (IOException exception) {
            throw new CucumberException(exception);
        }
        
        this.path = path;
        this.suffix = suffix;
        
        this.moveNext();
    }
    
    @Override
    public boolean hasNext() {
        return this.next != null;
    }
    
    @Override
    public Resource next() {
        try {
            if (this.hasNext()) {
                return this.next;
            }
            throw new NoSuchElementException();
        } finally {
            this.moveNext();
        }
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    private String getPathRelativeToClasspath(VirtualFile virtualFile) {
        return virtualFile.getPathName().replaceFirst("^.+(\\.jar|\\.war/WEB-INF/classes)/", "");
    }
    
    private boolean hasSuffix(String path) {
        return this.suffix == null || "".equals(this.suffix) || path.endsWith(this.suffix);
    }
    
    private void moveNext() {
        this.next = null;
        
        while(this.elements.hasNext()) {
            VirtualFile virtualFile = this.elements.next();
            String name = this.getPathRelativeToClasspath(virtualFile);
            
            if(name.startsWith(this.path) && this.hasSuffix(name)) {
                this.next = new Jbas7Resource(virtualFile, name);
                break;
            }
        }
    }
}
