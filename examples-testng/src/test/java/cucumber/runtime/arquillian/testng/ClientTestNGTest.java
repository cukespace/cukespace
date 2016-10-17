package cucumber.runtime.arquillian.testng;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.runtime.arquillian.front.ConstantServlet;
import org.apache.openejb.loader.IO;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.net.URL;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.testng.Assert.assertEquals;

public class ClientTestNGTest extends CukeSpace {
    @Deployment(testable = false)
    public static Archive<?> createDeployment() {
        return create(WebArchive.class).addClass(ConstantServlet.class);
    }

    @ArquillianResource
    private URL base;

    private String response;

    @Given("^I read \"([^\"]*)\"$")
    public void i_read(final String path) throws Throwable {
        response = IO.slurp(new URL(base.toExternalForm() + path));
    }

    @Then("^I have \"([^\"]*)\"$")
    public void i_have(final String expected) throws Throwable {
        assertEquals(response, expected);
    }
}
