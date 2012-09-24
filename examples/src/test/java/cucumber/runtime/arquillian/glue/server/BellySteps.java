package cucumber.runtime.arquillian.glue.server;

import static org.junit.Assert.assertEquals;

import cucumber.annotation.en.Given;
import cucumber.annotation.en.Then;
import cucumber.annotation.en.When;
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
