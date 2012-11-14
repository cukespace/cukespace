package cucumber.runtime.arquillian.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import cucumber.runtime.arquillian.domain.Belly;
import org.junit.Test;

public class BellyTest {
    @Test
    public void shouldBeHungry() {
        Belly belly = new Belly();
        belly.setCukes(0);
        assertTrue(belly.isHungry());
    }

    @Test
    public void shouldNotBeHungry() {
        Belly belly = new Belly();
        belly.setCukes(1);
        assertFalse(belly.isHungry());
    }
}
