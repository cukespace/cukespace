package cucumber.runtime.arquillian;

import org.junit.runners.model.InitializationError;

// just an alias
public class CukeSpace extends ArquillianCucumber {
    public CukeSpace(final Class<?> testClass) throws InitializationError {
        super(testClass);
    }
}
