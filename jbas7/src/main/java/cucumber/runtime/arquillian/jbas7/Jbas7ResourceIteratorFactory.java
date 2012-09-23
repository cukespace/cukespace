package cucumber.runtime.arquillian.jbas7;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.jboss.vfs.VirtualFile;

import cucumber.io.Resource;
import cucumber.io.ResourceIteratorFactory;
import cucumber.runtime.CucumberException;

public class Jbas7ResourceIteratorFactory implements ResourceIteratorFactory {
    
    @Override
    public Iterator<Resource> createIterator(URL url, String path, String suffix) {
        try {
            Object content = url.openConnection().getContent();
            
            if (content instanceof VirtualFile) {
                return new Jbas7ResourceIterator((VirtualFile) content, path, suffix);
            }
            
            throw new IllegalArgumentException(new StringBuilder()
                .append("URL ")
                .append(url)
                .append(" does not refer to a valid JBoss AS 7 resource")
                .toString());
        } catch (IOException exception) {
            throw new CucumberException(exception);
        }
    }
    
    @Override
    public boolean isFactoryFor(URL url) {
        
        return "vfs".equals(url.getProtocol());
    }
}
