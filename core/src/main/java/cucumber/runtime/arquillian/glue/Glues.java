package cucumber.runtime.arquillian.glue;

import cucumber.api.CucumberOptions;
import cucumber.api.StepDefinitionReporter;
import cucumber.runtime.DuplicateStepDefinitionException;
import cucumber.runtime.Glue;
import cucumber.runtime.HookDefinition;
import cucumber.runtime.StepDefinition;
import cucumber.runtime.StepDefinitionMatch;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.java.JavaBackend;
import gherkin.I18n;
import gherkin.formatter.model.Step;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

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

        final CucumberOptions cucumberOptions = clazz.getAnnotation(CucumberOptions.class);
        if (cucumberOptions != null) {
            final String[] cucumberGlues = cucumberOptions.glue();
            if (cucumberGlues.length > 0) {
                final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                final JavaBackend javaBackend = new JavaBackend(new MultiLoader(classLoader));
                final ScanGlue glue = new ScanGlue();
                javaBackend.loadGlue(glue, asList(cucumberGlues));

                glues.addAll(glue.classes);
            }
        }

        return glues;
    }

    private static class ScanGlue implements Glue {
        private final Set<Class<?>> classes = new HashSet<Class<?>>(); // make classes unique

        private static Class<?> clazz(final Object hookDefinition) {
            final Class<?> stepClass = hookDefinition.getClass();
            if (stepClass.getName().startsWith("cucumber.runtime.java.Java")) {
                try {
                    final Field f = stepClass.getDeclaredField("method");
                    if (!f.isAccessible()) {
                        f.setAccessible(true);
                    }
                    final Method m = Method.class.cast(f.get(hookDefinition));
                    return m.getDeclaringClass();
                } catch (final Exception e) {
                    // no-op
                }
            }
            return null;
        }

        private void addClazz(final Class<?> clazz) {
            if (clazz != null) {
                classes.add(clazz);
            }
        }

        @Override
        public void addStepDefinition(final StepDefinition stepDefinition) throws DuplicateStepDefinitionException {
            addClazz(clazz(stepDefinition));
        }

        @Override
        public void addBeforeHook(final HookDefinition hookDefinition) {
            addClazz(clazz(hookDefinition));
        }

        @Override
        public void addAfterHook(final HookDefinition hookDefinition) {
            addClazz(clazz(hookDefinition));
        }

        @Override
        public List<HookDefinition> getBeforeHooks() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<HookDefinition> getAfterHooks() {
            throw new UnsupportedOperationException();
        }

        @Override
        public StepDefinitionMatch stepDefinitionMatch(final String featurePath, final Step step, final I18n i18n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reportStepDefinitions(final StepDefinitionReporter stepDefinitionReporter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeScenarioScopedGlue() {
            // no-op
        }
    }
}
