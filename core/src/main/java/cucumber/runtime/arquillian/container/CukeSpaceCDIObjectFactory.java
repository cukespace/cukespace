package cucumber.runtime.arquillian.container;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.Collection;

// base class to ease custom lookups of steps
public class CukeSpaceCDIObjectFactory extends ContextualObjectFactoryBase {
    private final Collection<CreationalContext<?>> contexts = new ArrayList<>();

    @Override
    public <T> T getInstance(final Class<T> glueClass) {
        final BeanManager beanManager = CDI.current().getBeanManager();
        final Bean<?> bean = beanManager.resolve(beanManager.getBeans(glueClass));
        final CreationalContext<Object> creationalContext = beanManager.createCreationalContext(null);
        if (!beanManager.isNormalScope(bean.getScope())) {
            contexts.add(creationalContext);
        }
        return glueClass.cast(beanManager.getReference(bean, glueClass, creationalContext));
    }

    @Override
    public void stop() {
        for (final CreationalContext<?> cc : contexts) {
            cc.release();
        }
        contexts.clear();
    }
}
