package cucumber.runtime.arquillian.shared;

import cucumber.runtime.arquillian.api.event.AfterAfterHooks;
import cucumber.runtime.arquillian.api.event.AfterBeforeHooks;
import cucumber.runtime.arquillian.api.event.AfterStep;
import cucumber.runtime.arquillian.api.event.BeforeAfterHooks;
import cucumber.runtime.arquillian.api.event.BeforeBeforeHooks;
import cucumber.runtime.arquillian.api.event.BeforeStep;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

public class EventHelper {
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

    private static final ThreadLocal<EventHelper> CURRENT = new ThreadLocal<EventHelper>();

    public void capture(@Observes final BeforeClass ignored) {
        CURRENT.set(this);
    }

    public void reset(@Observes final AfterClass ignored) {
        CURRENT.remove();
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
