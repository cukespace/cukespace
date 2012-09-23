package cucumber.runtime.arquillian.jbas7;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.Test;

import cucumber.io.ResourceIteratorFactory;

/**
 * Tests for the {@link Jbas7ResourceIteratorFactory} class.
 */
public class Jbas7ResourceIteratorFactoryTest {
    
    @Test
    public void shouldBeFactoryForVfsUrl() throws URISyntaxException, MalformedURLException {
        String resourcePath = "cucumber/runtime/arquillian/jbas7/Jbas7Resource.class";
        URL resourceUrl = Jbas7ResourceTest.class.getResource("/" + resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        URL url = virtualFile.toURL();
        ResourceIteratorFactory factory = new Jbas7ResourceIteratorFactory();
        
        assertTrue(factory.isFactoryFor(url));
    }
    
    @Test
    public void shouldGetIteratorForVfsUrl() throws URISyntaxException, MalformedURLException {
        String resourcePath = "/cucumber/runtime/arquillian/client";
        URL resourceUrl = Jbas7ResourceTest.class.getResource(resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI().toString());
        URL url = virtualFile.toURL();
        ResourceIteratorFactory factory = new Jbas7ResourceIteratorFactory();
        
        assertThat(factory.createIterator(url, "cucumber", ".properties"), notNullValue());
    }
    
    @Test
    public void shouldNotBeFactoryForOtherUrls() throws URISyntaxException, MalformedURLException {
        URL url = this.getClass().getResource("/dummy/resources/dummy-resource.txt");
        ResourceIteratorFactory factory = new Jbas7ResourceIteratorFactory();
        
        assertFalse(factory.isFactoryFor(url));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGetIteratorForNonVfsUrl() throws URISyntaxException, MalformedURLException {
        URL url = this.getClass().getResource("/dummy/resources/dummy-resource.txt");
        ResourceIteratorFactory factory = new Jbas7ResourceIteratorFactory();
        
        factory.createIterator( url, "dummy", ".txt" );
    }
}
