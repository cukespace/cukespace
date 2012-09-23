package cucumber.runtime.arquillian.jbas7;

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

public class Jbas7ResourceTest {
    
    @Test
    public void shouldGetInputStreamForResource() throws URISyntaxException, IOException {
        String resourcePath = "/dummy/resources/dummy-resource.txt";
        URL resourceUrl = Jbas7ResourceTest.class.getResource(resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        Jbas7Resource resource = new Jbas7Resource(virtualFile, resourcePath);
        InputStream resourceStream = resource.getInputStream();
        
        try
        {
            assertThat(resourceStream, notNullValue());
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));
            
            assertThat(reader.readLine(), equalTo("This is a dummy resource."));
        }
        finally
        {
            resourceStream.close();
        }
    }
    
    @Test
    public void shouldReturnClassNameFromPath() throws URISyntaxException {
        String resourcePath = "cucumber/runtime/arquillian/jbas7/Jbas7Resource.class";
        URL resourceUrl = Jbas7ResourceTest.class.getResource("/" + resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        Jbas7Resource resource = new Jbas7Resource(virtualFile, resourcePath);
        String className = resource.getClassName();
        
        assertThat(className, equalTo("cucumber.runtime.arquillian.jbas7.Jbas7Resource"));
    }
    
    @Test
    public void shouldReturnOriginalPath() throws URISyntaxException {
        String resourcePath = "cucumber/runtime/arquillian/jbas7/Jbas7Resource.class";
        URL resourceUrl = Jbas7ResourceTest.class.getResource("/" + resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        Jbas7Resource resource = new Jbas7Resource(virtualFile, resourcePath);
        String actualPath = resource.getPath();
        
        assertThat(actualPath, equalTo(resourcePath));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionForGetClassNameWhenNotClassFile() throws URISyntaxException {
        String resourcePath = "/dummy/resources/dummy-resource.txt";
        URL resourceUrl = Jbas7ResourceTest.class.getResource(resourcePath);
        VirtualFile virtualFile = VFS.getChild(resourceUrl.toURI());
        Jbas7Resource resource = new Jbas7Resource(virtualFile, resourcePath);
        
        resource.getClassName();
    }
}
