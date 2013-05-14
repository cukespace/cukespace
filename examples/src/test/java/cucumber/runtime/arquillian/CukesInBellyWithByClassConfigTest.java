package cucumber.runtime.arquillian;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.api.junit.Cucumber;
import cucumber.runtime.arquillian.api.Features;
import cucumber.runtime.arquillian.domain.Belly;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;

@RunWith(ArquillianCucumber.class)
@Features("src/test/resources/cucumber/runtime/arquillian/feature/*.feature")
@Cucumber.Options(strict = true, tags = { "@test-tag" })
public class CukesInBellyWithByClassConfigTest {
    @Deployment
    public static Archive<?> createDeployment() {
        return create(WebArchive.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addClass(Belly.class)
            .addClass(CukesInBellyWithByClassConfigTest.class);
    }

    @Inject
    private Belly belly;

    @When("^I eat (\\d+) cukes$")
    public void eatCukes(int cukes) {
        belly.setCukes(cukes);
    }

    @Given("^I have a belly$")
    public void setUpBelly() {
        belly = new Belly();
    }

    @Then("^I should have (\\d+) cukes in my belly$")
    public void shouldHaveThisMany(int cukes) {
        assertEquals(cukes, belly.getCukes());
    }
}
