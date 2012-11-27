package cucumber.runtime.arquillian.weld;

import cucumber.runtime.arquillian.ObjectFactoryExtension;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class WeldObjectFactoryExtension extends Weld implements ObjectFactoryExtension {
    private WeldContainer weld;

    @Override
    public void start() {
        weld = initialize();
    }

    @Override
    public void stop() {
        shutdown();
    }

    @Override
    public void addClass(Class<?> clazz) {
        // intentionally cucumber.runtime.arquillian.ObjectFactoryExtension
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        return weld.instance().select(type).get();
    }
}
