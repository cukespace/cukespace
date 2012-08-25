package cucumber.runtime.arquillian.jbas7.test.unit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.Test;

import cucumber.runtime.arquillian.jbas7.Jbas7Resource;

/**
 * Unit tests for the {@link Jbas7Resource} class.
 */
public class Jbas7ResourceTest {
    
    /**
     * Initializes a new instance of the Jbas7ResourceTest class.
     */
    public Jbas7ResourceTest() {
        
        // intentionally empty
    }
    
    /**
     * Verifies that {@link Jbas7Resource#getInputStream()} returns an input
     * stream to the resource.
     * 
     * @throws URISyntaxException Thrown if the URI to the resource is invalid.
     * @throws IOException Thrown if an I/O error occurs when accessing the
     * resource.
     */
    @Test
    public void shouldGetInputStreamForResource() throws URISyntaxException, IOException {
        
        // Given
        String resourcePath = "/dummy/resources/dummy-resource.txt";
        URL resourceUrl = Jbas7ResourceTest.class.getResource(resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        Jbas7Resource resource = new Jbas7Resource(virtualFile, resourcePath);
        
        // When
        InputStream resourceStream = resource.getInputStream();
        
        try
        {
            // Then
            assertThat(resourceStream, notNullValue());
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));
            
            assertThat(reader.readLine(), equalTo("This is a dummy resource."));
        }
        finally
        {
            resourceStream.close();
        }
    }
    
    /**
     * Verifies that {@link Jbas7Resource#getClassName()} returns the
     * associated class name for the resource.
     * 
     * @throws URISyntaxException Thrown if the URI to the resource is invalid.
     */
    @Test
    public void shouldReturnClassNameFromPath() throws URISyntaxException {
        
        // Given
        String resourcePath = "cucumber/runtime/arquillian/jbas7/Jbas7Resource.class";
        URL resourceUrl = Jbas7ResourceTest.class.getResource("/" + resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        Jbas7Resource resource = new Jbas7Resource(virtualFile, resourcePath);
        
        // When
        String className = resource.getClassName();
        
        // Then
        assertThat(className, equalTo("cucumber.runtime.arquillian.jbas7.Jbas7Resource"));
    }
    
    /**
     * Verifies that {@link Jbas7Resource#getPath()} returns the original
     * resource path.
     * 
     * @throws URISyntaxException Thrown if the URI to the resource is invalid.
     */
    @Test
    public void shouldReturnOriginalPath() throws URISyntaxException {
        
        // Given
        String resourcePath = "cucumber/runtime/arquillian/jbas7/Jbas7Resource.class";
        URL resourceUrl = Jbas7ResourceTest.class.getResource("/" + resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        Jbas7Resource resource = new Jbas7Resource(virtualFile, resourcePath);
        
        // When
        String actualPath = resource.getPath();
        
        // Then
        assertThat(actualPath, equalTo(resourcePath));
    }
    
    /**
     * Verifies that {@link Jbas7Resource#getClassName()} throws an
     * IllegalArgumentException for resources that are not class files.
     * 
     * @throws URISyntaxException Thrown if the URI to the resource is invalid.
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionForGetClassNameWhenNotClassFile() throws URISyntaxException {
        
        // Given
        String resourcePath = "/dummy/resources/dummy-resource.txt";
        URL resourceUrl = Jbas7ResourceTest.class.getResource(resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        Jbas7Resource resource = new Jbas7Resource(virtualFile, resourcePath);
        
        // When
        resource.getClassName();
    }
}
