package info.loganmcgrath.cucumber.arquillian.domain;


/**
 * Belly for eating cukes.
 */
public class Belly
{
    /**
     * The number of cukes.
     */
    private int cukes;
    
    
    /**
     * Initializes a new instance of the Belly class.
     */
    public Belly()
    {
        // intentionally empty
        
    } // Belly
    
    
    /**
     * Gets the number of cukes.
     * 
     * @return The number of cukes.
     */
    public int getCukes()
    {
        return this.cukes;
        
    } // getCukes
    
    
    /**
     * Sets the number of cukes.
     * 
     * @param cukes The number of cukes.
     */
    public void setCukes( int cukes )
    {
        this.cukes = cukes;
        
    } // setCukes
    
    
} // class Belly
