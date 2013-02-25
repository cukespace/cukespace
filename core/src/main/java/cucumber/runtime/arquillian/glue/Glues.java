package cucumber.runtime.arquillian.glue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class Glues {
    private Glues() {
        // no-op
    }

    public static Collection<Class<?>> findGlues(final Class<?> clazz) {
        final Collection<Class<?>> glues = new ArrayList<Class<?>>();

        final cucumber.runtime.arquillian.api.Glues additionalGlues = clazz.getAnnotation(cucumber.runtime.arquillian.api.Glues.class);
        if (additionalGlues != null) {
            Collections.addAll(glues, additionalGlues.value());
        }

        return glues;
    }
}
