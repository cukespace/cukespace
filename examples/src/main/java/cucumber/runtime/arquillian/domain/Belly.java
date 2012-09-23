package cucumber.runtime.arquillian.domain;

import java.io.Serializable;

public class Belly implements Serializable {
    
    private static final long serialVersionUID = -1182468379585134676L;
    
    private int cukes;
    
    public int getCukes() {
        return this.cukes;
    }
    
    public boolean isHungry() {
        return this.cukes <= 0;
    }
    
    public void setCukes(int cukes) {
        this.cukes = cukes;
    }
}
