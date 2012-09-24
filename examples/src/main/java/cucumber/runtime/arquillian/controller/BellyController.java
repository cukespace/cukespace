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

@Named
@SessionScoped
public class BellyController implements Serializable {
    
    private static final String ERROR_HUNGRY = "The belly is still HUNGRY!";
    
    private static final long serialVersionUID = 1760736145267516537L;
    
    private static final String SUCCESS_NOT_HUNGRY = "The belly ate {0} cukes!";
    
    private Belly belly;
    
    @Inject
    private FacesContext facesContext;
    
    public BellyController() {
        belly = new Belly();
    }
    
    public String eatCukes() {
        if(belly.isHungry()) {
            facesContext.addMessage(null, new FacesMessage(SEVERITY_ERROR, ERROR_HUNGRY, ERROR_HUNGRY));
        } else {
            String message = MessageFormat.format(SUCCESS_NOT_HUNGRY, belly.getCukes());
            facesContext.addMessage(null, new FacesMessage(SEVERITY_INFO, message, message));
        }
        return "belly.xhtml";
    }
    
    @Produces
    public Belly getCurrentBelly() {
        return belly;
    }
}
