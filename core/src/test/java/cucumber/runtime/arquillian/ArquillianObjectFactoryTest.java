package cucumber.runtime.arquillian;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.test.spi.TestEnricher;
import org.junit.Test;

public class ArquillianObjectFactoryTest {

    @Test
    public void shouldEnrichInstanceOnlyOnce() {
        ArquillianObjectFactory factory = new ArquillianObjectFactory();
        Set<TestEnricher> testEnrichers = new HashSet<TestEnricher>();
        TestEnricher testEnricher = mock(TestEnricher.class);

        testEnrichers.add(testEnricher);
        TestEnricherProvider.setTestEnrichers(testEnrichers);

        Object instance1 = factory.getInstance(Object.class);

        factory.getInstance(Object.class);
        factory.getInstance(Object.class);

        verify(testEnricher, times(1)).enrich(instance1);
    }

    @Test
    public void shouldGetNewInstance() {
        ArquillianObjectFactory factory = new ArquillianObjectFactory();
        Set<TestEnricher> testEnrichers = new HashSet<TestEnricher>();

        TestEnricherProvider.setTestEnrichers(testEnrichers);

        Object instance1 = factory.getInstance(Object.class);

        factory.stop();
        factory.start();

        Object instance2 = factory.getInstance(Object.class);

        assertThat(instance1, not(sameInstance(instance2)));
    }

    @Test
    public void shouldGetSameInstance() {
        ArquillianObjectFactory factory = new ArquillianObjectFactory();
        Set<TestEnricher> testEnrichers = new HashSet<TestEnricher>();

        TestEnricherProvider.setTestEnrichers(testEnrichers);

        Object instance1 = factory.getInstance(Object.class);
        Object instance2 = factory.getInstance(Object.class);

        assertThat(instance1, sameInstance(instance2));
    }
}
