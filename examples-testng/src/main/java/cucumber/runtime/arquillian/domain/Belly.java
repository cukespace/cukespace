package cucumber.runtime.arquillian.domain;

public class Belly {
    private int cukes;

    public int getCukes() {
        return cukes;
    }

    public boolean isHungry() {
        return cukes <= 0;
    }

    public void setCukes(int cukes) {
        this.cukes = cukes;
    }
}
