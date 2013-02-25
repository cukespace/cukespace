package cucumber.runtime.arquillian.api;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Target(TYPE)
@Retention(RUNTIME)
public @interface Glues {
    /**
     * These classes doesn't need to be in the Archive since it will be added automatically.
     *
     * @return list of glues (excluding the test class)
     */
    Class<?>[] value();
}
