package cucumber.runtime.arquillian;

import static java.lang.String.format;

import cucumber.runtime.CucumberException;

public class DefaultObjectFactoryExtension implements ObjectFactoryExtension {
    public void addClass(Class<?> clazz) {
        // intentionally empty
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        try {
            return type.getConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new CucumberException(format("Failed to instantiate %s", type), exception);
        }
    }

    public void start() {
        // intentionally empty
    }

    @Override
    public void stop() {
        // intentionally empty
    }
}
