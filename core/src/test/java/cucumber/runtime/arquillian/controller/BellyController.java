package cucumber.runtime.arquillian.controller;

import static javax.faces.application.FacesMessage.*;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import cucumber.runtime.arquillian.domain.Belly;

/**
 * Controller for the cuke-eating belly.
 */
@Named
@SessionScoped
public class BellyController {
    
    /**
     * Error message used when the belly is still hungry.
     */
    private static final String ERROR_HUNGRY = "The belly is still HUNGRY!";
    
    /**
     * Success message used when the belly is not hungry.
     */
    private static final String SUCCESS_NOT_HUNGRY = "The belly ate {0} cukes!";
    
    /**
     * The number of cukes.
     */
    private Belly belly;
    
    /**
     * The faces context.
     */
    @Inject
    private FacesContext facesContext;
    
    /**
     * Initializes a new instance of the BellyController class.
     */
    public BellyController() {
        this.belly = new Belly();
    }
    
    /**
     * Eats the cukes.
     * 
     * @return The next view ID.
     */
    public String eatCukes() {
        if(this.belly.isNotHungry()) {
            this.facesContext.addMessage(null, new FacesMessage(SEVERITY_INFO, SUCCESS_NOT_HUNGRY, SUCCESS_NOT_HUNGRY));
        } else {
            this.facesContext.addMessage(null, new FacesMessage(SEVERITY_ERROR, ERROR_HUNGRY, ERROR_HUNGRY));
        }
        return "belly.xhtml";
    }
    
    /**
     * Gets the current belly.
     * 
     * @return The belly.
     */
    @Produces
    public Belly getCurrentBelly() {
        return this.belly;
    }
    
    /**
     * Sets the number of cukes in the belly.
     * 
     * @param cukes The number of cukes.
     */
    public void setCukes(int cukes) {
        this.belly.setCukes(cukes);
    }
}
