package cucumber.runtime.arquillian.glue.server;

import static org.junit.Assert.assertEquals;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.arquillian.domain.Belly;

public class BellySteps {
    
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
