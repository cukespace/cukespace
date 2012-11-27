package cucumber.runtime.arquillian;

import java.util.Iterator;
import java.util.ServiceLoader;
import cucumber.runtime.CucumberException;
import cucumber.runtime.java.ObjectFactory;

public class ArquillianObjectFactory implements ObjectFactory {
    private final ObjectFactoryExtension extension;

    public ArquillianObjectFactory() {
        Iterator<ObjectFactoryExtension> iterator = ServiceLoader.load(ObjectFactoryExtension.class).iterator();
        if (iterator.hasNext()) {
            extension = iterator.next();
            if (iterator.hasNext()) {
                throw new CucumberException("Multiple implementations of ObjectFactoryExtension found");
            }
        } else {
            extension = new DefaultObjectFactoryExtension();
        }
    }

    @Override
    public void start() {
        extension.start();
    }

    @Override
    public void stop() {
        extension.stop();
    }

    @Override
    public void addClass(Class<?> clazz) {
        extension.addClass(clazz);
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        return extension.getInstance(type);
    }
}
