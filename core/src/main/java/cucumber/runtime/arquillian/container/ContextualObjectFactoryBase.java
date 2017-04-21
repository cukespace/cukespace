package cucumber.runtime.arquillian.container;

import cucumber.api.java.ObjectFactory;

// base class to ease custom lookups of steps
public abstract class ContextualObjectFactoryBase implements ObjectFactory {
    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        // no-op
    }

    @Override
    public boolean addClass(final Class<?> glueClass) {
        return true;
    }
}
