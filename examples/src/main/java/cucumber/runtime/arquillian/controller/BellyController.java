package cucumber.runtime.arquillian.controller;

import static javax.faces.application.FacesMessage.SEVERITY_ERROR;
import static javax.faces.application.FacesMessage.SEVERITY_INFO;

import java.io.Serializable;
import java.text.MessageFormat;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import cucumber.runtime.arquillian.domain.Belly;

@Named
@SessionScoped
public class BellyController implements Serializable {
    public static final String MSG_ERROR_HUNGRY = "The belly is still HUNGRY!";
    public static final String MSG_SUCCESS_NOT_HUNGRY = "The belly ate {0} cukes!";
    private static final long serialVersionUID = 1760736145267516537L;

    @Inject
    @New
    private Belly belly;
    
    @Inject
    private FacesContext facesContext;

    public BellyController() {
        // intentionally empty
    }

    public BellyController(FacesContext facesContext, Belly belly) {
        this.facesContext = facesContext;
        this.belly = belly;
    }

    public String eatCukes() {
        if(belly.isHungry()) {
            facesContext.addMessage(null, new FacesMessage(SEVERITY_ERROR, MSG_ERROR_HUNGRY, MSG_ERROR_HUNGRY));
        } else {
            String message = MessageFormat.format(MSG_SUCCESS_NOT_HUNGRY, belly.getCukes());
            facesContext.addMessage(null, new FacesMessage(SEVERITY_INFO, message, message));
        }
        return "belly";
    }
    
    @Produces
    public Belly getCurrentBelly() {
        return belly;
    }
}
