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
            elements = virtualFile.getChildrenRecursively().iterator();
        } catch (IOException exception) {
            throw new CucumberException(exception);
        }
        
        this.path = path;
        this.suffix = suffix;
        
        moveNext();
    }
    
    @Override
    public boolean hasNext() {
        return next != null;
    }
    
    @Override
    public Resource next() {
        try {
            if (this.hasNext()) {
                return next;
            }
            throw new NoSuchElementException();
        } finally {
            moveNext();
        }
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    private String getPathRelativeToClasspath(VirtualFile virtualFile) {
        return virtualFile.getPathName().replaceFirst("^.+(\\.jar|\\.war/WEB-INF/classes)/", "");
    }
    
    private boolean hasSuffix(String pathToCheck) {
        return suffix == null || "".equals(suffix) || pathToCheck.endsWith(suffix);
    }
    
    private void moveNext() {
        next = null;
        
        while(elements.hasNext()) {
            VirtualFile virtualFile = elements.next();
            String name = getPathRelativeToClasspath(virtualFile);
            
            if(name.startsWith(path) && hasSuffix(name)) {
                next = new Jbas7Resource(virtualFile, name);
                break;
            }
        }
    }
}
