package cucumber.runtime.arquillian.controller;

import static javax.faces.application.FacesMessage.*;

import java.io.Serializable;
import java.text.MessageFormat;

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
public class BellyController implements Serializable {
    
    /**
     * Error message used when the belly is still hungry.
     */
    private static final String ERROR_HUNGRY = "The belly is still HUNGRY!";
    
    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1760736145267516537L;
    
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
        if(this.belly.isHungry()) {
            this.facesContext.addMessage(null, new FacesMessage(SEVERITY_ERROR, ERROR_HUNGRY, ERROR_HUNGRY));
        } else {
            String message = MessageFormat.format(SUCCESS_NOT_HUNGRY, this.belly.getCukes());
            this.facesContext.addMessage(null, new FacesMessage(SEVERITY_INFO, message, message));
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
}
