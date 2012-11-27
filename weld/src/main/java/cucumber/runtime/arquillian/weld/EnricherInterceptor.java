package cucumber.runtime.arquillian.weld;

import static cucumber.runtime.arquillian.TestEnricherProvider.getTestEnrichers;

import javax.annotation.PostConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.jboss.arquillian.test.spi.TestEnricher;

@Enriched
@Interceptor
public class EnricherInterceptor {
    @PostConstruct
    public void enrich(InvocationContext context) throws Exception {
        for (TestEnricher enricher : getTestEnrichers()) {
            enricher.enrich(context.getTarget());
        }
        context.proceed();
    }
}
