package cucumber.runtime.arquillian.feature;

import cucumber.api.java.fr.Alors;
import cucumber.api.java.fr.Etantdonnée;
import cucumber.api.java.fr.Quand;
import cucumber.runtime.arquillian.ArquillianCucumber;
import cucumber.runtime.arquillian.domain.Belly;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;

@RunWith(ArquillianCucumber.class)
public class CukesInBellyFrTest {
    @Deployment
    public static Archive<?> createDeployment() {
        return create(WebArchive.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addClass(Belly.class);
    }

    @Inject
    private Belly belly;

    @Quand("^I eat (\\d+) cukes$")
    public void eatCukes(int cukes) {
        belly.setCukes(cukes);
    }

    @Etantdonnée("^I have a belly$")
    public void setUpBelly() {
        belly = new Belly();
    }

    @Alors( "^I should have (\\d+) cukes in my belly$")
    public void shouldHaveThisMany(int cukes) {
        assertEquals(cukes, belly.getCukes());
    }
}
