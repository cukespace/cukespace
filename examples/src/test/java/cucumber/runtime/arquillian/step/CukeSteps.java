package cucumber.runtime.arquillian.step;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.arquillian.domain.Belly;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CukeSteps {
    @Inject
    private Belly belly;

    @When("^I eat (\\d+) cukes$")
    public void eatCukes(int cukes) {
        belly.setCukes(cukes);
    }

    @Given("^I have a belly$")
    public void setUpBelly() {
        assertNotNull(belly);
    }

    @Then("^I should have (\\d+) cukes in my belly$")
    public void shouldHaveThisMany(int cukes) {
        assertEquals(cukes, belly.getCukes());
    }
}
