package cucumber.runtime.arquillian;

public interface ObjectFactoryExtension {
    void start();

    void stop();

    void addClass(Class<?> clazz);

    <T> T getInstance(Class<T> type);
}
