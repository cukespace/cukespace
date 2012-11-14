package cucumber.runtime.arquillian.jbas7;

import static java.lang.String.format;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import cucumber.runtime.CucumberException;
import cucumber.runtime.io.Resource;
import cucumber.runtime.io.ResourceIteratorFactory;
import org.jboss.vfs.VirtualFile;

public class Jbas7ResourceIteratorFactory implements ResourceIteratorFactory {
    @Override
    public Iterator<Resource> createIterator(URL url, String path, String suffix) {
        try {
            Object content = url.openConnection().getContent();
            if (content instanceof VirtualFile) {
                return new Jbas7ResourceIterator((VirtualFile) content, path, suffix);
            }
            throw new IllegalArgumentException(format("URL %s does not refer to a valid JBoss AS 7 resource", url));
        } catch (IOException exception) {
            throw new CucumberException(exception);
        }
    }
    
    @Override
    public boolean isFactoryFor(URL url) {
        return "vfs".equals(url.getProtocol());
    }
}
