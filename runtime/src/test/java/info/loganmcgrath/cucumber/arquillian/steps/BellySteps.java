package info.loganmcgrath.cucumber.arquillian.steps;

import static org.junit.Assert.assertEquals;

import cucumber.annotation.en.Given;
import cucumber.annotation.en.Then;
import cucumber.annotation.en.When;
import info.loganmcgrath.cucumber.arquillian.domain.Belly;


/**
 * Test steps for the cuke-eating belly.
 */
public class BellySteps
{
    /**
     * The belly under test.
     */
    private Belly belly;
    
    
    /**
     * Initializes a new instance of the BellySteps class.
     */
    public BellySteps()
    {
        // intentionally empty
        
    } // BellySteps
    
    
    /**
     * Sets up the belly for eating cukes.
     */
    @Given( "^I have a belly$" )
    public void setUpBelly()
    {
        this.belly = new Belly();
        
    } // setUpBelly
    
    
    /**
     * Eats some cukes.
     * 
     * @param cukes The number of cukes.
     */
    @When( "^I eat (\\d+) cukes$" )
    public void eatCukes( int cukes )
    {
        this.belly.setCukes( cukes );
        
    } // eatCukes
    
    
    /**
     * Verifies the number of cukes.
     * 
     * @param cukes The expected number of cukes.
     */
    @Then( "^I have (\\d+) cukes in my belly$" )
    public void shouldHaveThisMany( int cukes )
    {
        assertEquals( cukes, this.belly.getCukes() );
        
    } // shouldHaveThisMany
    
    
} // class BellySteps
