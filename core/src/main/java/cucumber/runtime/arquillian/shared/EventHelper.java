package cucumber.runtime.arquillian.shared;

import cucumber.runtime.StepDefinition;
import cucumber.runtime.StepDefinitionMatch;
import cucumber.runtime.arquillian.api.event.AfterAfterHooks;
import cucumber.runtime.arquillian.api.event.AfterBeforeHooks;
import cucumber.runtime.arquillian.api.event.AfterStep;
import cucumber.runtime.arquillian.api.event.BeforeAfterHooks;
import cucumber.runtime.arquillian.api.event.BeforeBeforeHooks;
import cucumber.runtime.arquillian.api.event.BeforeStep;
import cucumber.runtime.arquillian.backend.ArquillianStepDefinition;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.TestEvent;

import java.lang.reflect.Field;

public class EventHelper {
    private static final ThreadLocal<TestEvent> TEST_EVENT = new ThreadLocal<TestEvent>();
    private static final ThreadLocal<EventHelper> CURRENT = new ThreadLocal<EventHelper>();

    @Inject
    private Event<BeforeBeforeHooks> beforeBeforeHooksEvent;

    @Inject
    private Event<AfterBeforeHooks> afterBeforeHooksEvent;

    @Inject
    private Event<BeforeAfterHooks> beforeAfterHooksEvent;

    @Inject
    private Event<AfterAfterHooks> afterAfterHooksEvent;

    @Inject
    private Event<BeforeStep> beforeStepEvent;

    @Inject
    private Event<AfterStep> afterStepEvent;

    public void capture(@Observes final BeforeClass ignored) {
        CURRENT.set(this);
    }

    public void reset(@Observes final AfterClass ignored) {
        CURRENT.remove();
    }

    public static void matched(final StepDefinitionMatch match) {
        final TestEvent event;
        try {
            final Field field = StepDefinitionMatch.class.getDeclaredField("stepDefinition");
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            final StepDefinition stepDefinition = StepDefinition.class.cast(field.get(match));
            if (ArquillianStepDefinition.class.isInstance(stepDefinition)) {
                final ArquillianStepDefinition arquillianStepDefinition = ArquillianStepDefinition.class.cast(stepDefinition);
                event = new TestEvent(arquillianStepDefinition.getInstance(), arquillianStepDefinition.getMethod());
                TEST_EVENT.set(event);
            } else { // mock to still fire events but just as marker, TODO: don't use TestEvent? -> java 8
                event = new TestEvent(TEST_EVENT, EventHelper.class.getMethod("currentEvent"));
            }
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        TEST_EVENT.set(event);
    }

    public static void unmatch() {
        TEST_EVENT.remove();
    }

    public static TestEvent currentEvent() {
        return TEST_EVENT.get();
    }

    // we can't get injected arquillian Manager so using this facade
    public static void fire(final Object event) {
        final EventHelper mgr = CURRENT.get();
        if (mgr == null) {
            CURRENT.remove();
            return;
        }

        final Class<?> cl = event.getClass();
        if (cl == BeforeBeforeHooks.class) {
            mgr.beforeBeforeHooksEvent.fire(BeforeBeforeHooks.class.cast(event));
        } else if (cl == AfterBeforeHooks.class) {
            mgr.afterBeforeHooksEvent.fire(AfterBeforeHooks.class.cast(event));
        } else if (cl == BeforeAfterHooks.class) {
            mgr.beforeAfterHooksEvent.fire(BeforeAfterHooks.class.cast(event));
        } else if (cl == AfterAfterHooks.class) {
            mgr.afterAfterHooksEvent.fire(AfterAfterHooks.class.cast(event));
        } else if (cl == BeforeStep.class) {
            mgr.beforeStepEvent.fire(BeforeStep.class.cast(event));
        } else if (cl == AfterStep.class) {
            mgr.afterStepEvent.fire(AfterStep.class.cast(event));
        } else {
            throw new IllegalArgumentException("Unsupported event: " + event);
        }
    }
}
