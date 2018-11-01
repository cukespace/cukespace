package cucumber.runtime.arquillian.api;

import cucumber.runtime.io.ResourceLoader;

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
public @interface Features {
    /**
     * @return feature paths
     */
    String[] value() default {};

    /**
     * @return custom loaders
     */
    Class<? extends ResourceLoader>[] loaders() default {};
}
