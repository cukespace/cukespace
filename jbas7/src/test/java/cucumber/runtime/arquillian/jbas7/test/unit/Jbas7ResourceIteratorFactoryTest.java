package cucumber.runtime.arquillian.jbas7.test.unit;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.Test;

import cucumber.io.Resource;
import cucumber.io.ResourceIteratorFactory;
import cucumber.runtime.arquillian.jbas7.Jbas7ResourceIteratorFactory;

/**
 * Tests for the {@link Jbas7ResourceIteratorFactory} class.
 */
public class Jbas7ResourceIteratorFactoryTest {
    
    /**
     * Initializes a new instance of the Jbas7ResourceIteratorFactoryTest class.
     */
    public Jbas7ResourceIteratorFactoryTest() {
        
        // intentionally empty
    }
    
    /**
     * Verifies that the factory reports that it works for JBoss VFS URL's.
     * 
     * @throws URISyntaxException Thrown for bad URI syntax.
     * @throws MalformedURLException Thrown for bad URL formation.
     */
    @Test
    public void shouldBeFactoryForVfsUrl() throws URISyntaxException, MalformedURLException {
        
        // Given
        String resourcePath = "cucumber/runtime/arquillian/jbas7/Jbas7Resource.class";
        URL resourceUrl = Jbas7ResourceTest.class.getResource("/" + resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        URL url = virtualFile.toURL();
        ResourceIteratorFactory factory = new Jbas7ResourceIteratorFactory();
        
        // When
        boolean factoryForUrl = factory.isFactoryFor(url);
        
        // Then
        assertTrue(factoryForUrl);
    }
    
    /**
     * Verifies that the factory will return an iterator for a VFS URL.
     * 
     * @throws URISyntaxException Thrown for bad URI syntax.
     * @throws MalformedURLException Thrown for bad URL formation.
     */
    @Test
    public void shouldGetIteratorForVfsUrl() throws URISyntaxException, MalformedURLException {
        
        // Given
        String resourcePath = "/cucumber/runtime/arquillian/client";
        URL resourceUrl = Jbas7ResourceTest.class.getResource(resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        URL url = virtualFile.toURL();
        ResourceIteratorFactory factory = new Jbas7ResourceIteratorFactory();
        
        // When
        Iterator<Resource> iterator = factory.createIterator(url, "cucumber", ".properties");
        
        // Then
        assertThat(iterator, notNullValue());
    }
    
    /**
     * Verifies that the factory does not report that it handles non-VFS URL's.
     * 
     * @throws URISyntaxException Thrown for bad URI syntax.
     * @throws MalformedURLException Thrown for bad URL formation.
     */
    @Test
    public void shouldNotBeFactoryForOtherUrls() throws URISyntaxException, MalformedURLException {
        
        // Given
        URL url = this.getClass().getResource("/dummy/resources/dummy-resource.txt");
        ResourceIteratorFactory factory = new Jbas7ResourceIteratorFactory();
        
        // When
        boolean factoryForUrl = factory.isFactoryFor(url);
        
        // Then
        assertFalse(factoryForUrl);
    }
    
    /**
     * Verifies that any attempt to get an iterator for any non-VFS URL will
     * cause an IllegalArgumentException.
     * 
     * @throws URISyntaxException Thrown for bad URI syntax.
     * @throws MalformedURLException Thrown for bad URL formation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGetIteratorForNonVfsUrl() throws URISyntaxException, MalformedURLException {
        
        // Given
        URL url = this.getClass().getResource("/dummy/resources/dummy-resource.txt");
        ResourceIteratorFactory factory = new Jbas7ResourceIteratorFactory();
        
        // When
        factory.createIterator( url, "dummy", ".txt" );
    }
}
