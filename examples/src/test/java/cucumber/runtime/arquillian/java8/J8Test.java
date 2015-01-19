package cucumber.runtime.arquillian.java8;

import cucumber.api.java8.En;
import cucumber.runtime.arquillian.CukeSpace;
import cucumber.runtime.arquillian.api.Lambda;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(CukeSpace.class)
public class J8Test implements En, Lambda {
    @Deployment
    public static Archive<?> war() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClass(Person.class);
    }

    @Inject
    private Person person;

    @Override
    public void define() {
        Given("I have a person", () -> assertNotNull(person));
        When("I say \"(.*)\"", person::say);
        Then("I should have said \"(.*)\"", (String hi) -> assertEquals(hi, person.getValue()));
    }

    @ApplicationScoped
    public static class Person {
        private String value;

        public String say(final String word) {
            value = word;
            return word;
        }

        public String getValue() {
            return value;
        }
    }
}
