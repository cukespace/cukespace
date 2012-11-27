package cucumber.runtime.arquillian.weld;

import java.util.HashMap;
import java.util.Map;
import cucumber.runtime.arquillian.ObjectFactoryExtension;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class WeldObjectFactoryExtension extends Weld implements ObjectFactoryExtension {
    private Map<Class<?>, Object> instances;
    private WeldContainer weld;

    public WeldObjectFactoryExtension() {
        instances = new HashMap<Class<?>, Object>();
    }

    @Override
    public void start() {
        weld = initialize();
    }

    @Override
    public void stop() {
        shutdown();
        instances.clear();
    }

    @Override
    public void addClass(Class<?> clazz) {
        // intentionally cucumber.runtime.arquillian.ObjectFactoryExtension
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInstance(Class<T> type) {
        if (!instances.containsKey(type)) {
            instances.put(type, weld.instance().select(type).get());
        }
        return (T) instances.get(type);
    }
}
