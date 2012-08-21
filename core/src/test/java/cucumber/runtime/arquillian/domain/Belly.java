package cucumber.runtime.arquillian.domain;

/**
 * Belly for eating cukes.
 */
public class Belly {
    
    /**
     * The number of cukes.
     */
    private int cukes;
    
    /**
     * Initializes a new instance of the Belly class.
     */
    public Belly() {
        // intentionally empty
    }
    
    /**
     * Gets the number of cukes.
     * 
     * @return The number of cukes.
     */
    public int getCukes() {
        return this.cukes;
    }
    
    /**
     * Gets a value indicating whether the belly is hungry.
     * 
     * @return True if the belly is hungry.
     */
    public boolean isNotHungry() {
        return this.cukes > 0;
    }
    
    /**
     * Sets the number of cukes.
     * 
     * @param cukes The number of cukes.
     */
    public void setCukes(int cukes) {
        this.cukes = cukes;
    }
}
