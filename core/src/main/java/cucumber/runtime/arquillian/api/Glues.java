package cucumber.runtime.arquillian.api;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 
 * @deprecated use CucumberOptions instead.
 */
@Deprecated
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
public @interface Glues {
    Class<?>[] value();
}
