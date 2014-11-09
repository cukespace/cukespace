package cucumber.runtime.arquillian.shared;

import cucumber.runtime.arquillian.api.event.AfterStep;
import cucumber.runtime.arquillian.api.event.BeforeStep;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.persistence.core.configuration.PersistenceConfiguration;
import org.jboss.arquillian.persistence.core.event.AfterPersistenceTest;
import org.jboss.arquillian.persistence.core.event.BeforePersistenceTest;
import org.jboss.arquillian.persistence.core.metadata.MetadataExtractor;
import org.jboss.arquillian.persistence.core.metadata.PersistenceExtensionEnabler;
import org.jboss.arquillian.persistence.core.metadata.PersistenceExtensionFeatureResolver;
import org.jboss.arquillian.persistence.core.metadata.PersistenceExtensionScriptingFeatureResolver;
import org.jboss.arquillian.persistence.script.configuration.ScriptingConfiguration;
import org.jboss.arquillian.test.spi.annotation.TestScoped;

public class PersistenceExtensionIntegration {
    private static Boolean IS_ON;

    public static boolean isOn() {
        if (IS_ON == null) {
            try {
                Class.forName("org.jboss.arquillian.persistence.core.event.BeforePersistenceTest", false, Thread.currentThread().getContextClassLoader());
                IS_ON = true;
            } catch (final ClassNotFoundException e) {
                IS_ON = false;
            }
        }
        return IS_ON;
    }

    public static class Observer {
        @Inject
        private Event<BeforePersistenceTest> beforePersistenceTestEvent;

        @Inject
        private Event<AfterPersistenceTest> afterPersistenceTestEvent;

        @Inject
        private Instance<PersistenceConfiguration> configurationInstance;

        @Inject @TestScoped
        private InstanceProducer<PersistenceExtensionFeatureResolver> persistenceExtensionFeatureResolverProvider;

        @Inject @TestScoped
        private InstanceProducer<PersistenceExtensionScriptingFeatureResolver> persistenceExtensionScriptingFeatureResolverProvider;

        @Inject
        private Instance<ScriptingConfiguration> scriptingConfigurationInstance;

        private final ThreadLocal<Boolean> active = new ThreadLocal<Boolean>();

        public void before(@Observes final BeforeStep event) {
            final MetadataExtractor extractor = new MetadataExtractor(event.getTestClass());
            final boolean isActive = new PersistenceExtensionEnabler(extractor).shouldPersistenceExtensionBeActivated();
            active.set(isActive);

            if (isActive) {
                final PersistenceConfiguration persistenceConfiguration = configurationInstance.get();
                persistenceExtensionFeatureResolverProvider.set(new PersistenceExtensionFeatureResolver(event.getTestMethod(), extractor, persistenceConfiguration));
                persistenceExtensionScriptingFeatureResolverProvider.set(new PersistenceExtensionScriptingFeatureResolver(event.getTestMethod(), extractor, scriptingConfigurationInstance.get()));

                beforePersistenceTestEvent.fire(new BeforePersistenceTest(event));
            }
        }

        public void after(@Observes final AfterStep event) {
            if (active.get()) {
                afterPersistenceTestEvent.fire(new AfterPersistenceTest(event));
            }
            active.remove();
        }
    }
}
