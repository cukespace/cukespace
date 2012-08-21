package cucumber.runtime.arquillian.producer;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;


/**
 * Produces the faces context for each injection.
 */
public class FacesContextProducer {
    
    /**
     * Initializes a new instance of the FacesContextProducer class.
     */
    public FacesContextProducer() {
        // intentionally empty
    }
    
    /**
     * Gets the current faces context.
     * 
     * @return The faces context.
     */
    @Produces
    @RequestScoped
    public FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }
}
