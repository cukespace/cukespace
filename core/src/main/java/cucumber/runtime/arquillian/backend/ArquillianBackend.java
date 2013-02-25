package cucumber.runtime.arquillian.backend;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.runtime.Backend;
import cucumber.runtime.CucumberException;
import cucumber.runtime.DuplicateStepDefinitionException;
import cucumber.runtime.Glue;
import cucumber.runtime.UnreportedStepExecutor;
import cucumber.runtime.Utils;
import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
import cucumber.runtime.java.StepDefAnnotation;
import cucumber.runtime.snippets.SnippetGenerator;
import gherkin.formatter.model.Step;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// patched to use the resource loader defined by this extension
// the best would probably to update cucumber-core to handle
// completely listed feature/steps (glue) classes/resources
public class ArquillianBackend implements Backend {
    private final SnippetGenerator snippetGenerator = new SnippetGenerator(new ArquillianSnippet());
    private final Map<Class<?>, Object> instances = new HashMap<Class<?>, Object>();
    private final Collection<Class<?>> glues = new ArrayList<Class<?>>();
    private Glue glue;

    public ArquillianBackend(final Collection<Class<?>> classes, final Class<?> clazz, final Object testInstance) {
        instances.put(clazz, testInstance);
        glues.addAll(classes);
    }

    @Override
    public void loadGlue(final Glue glue, final List<String> gluePaths) {
        this.glue = glue;
        initInstances();
        scan(); // dedicated scanning
    }

    private void initInstances() {
        for (final Class<?> glueClass : glues) {
            final Object instance;
            try {
                instance = glueClass.newInstance();
            } catch (final Exception e) {
                throw new IllegalArgumentException("Can't instantiate " + glueClass.getName(), e);
            }

            instances.put(glueClass, CucumberLifecycle.enrich(instance));
        }
    }

    private void scan() {
        for (final Map.Entry<Class<?>, Object> clazz : instances.entrySet()) {
            for (final Method method : clazz.getKey().getMethods()) {
                for (final Class<? extends Annotation> cucumberAnnotationClass : CucumberLifecycle.cucumberAnnotations()) {
                    final Annotation annotation = method.getAnnotation(cucumberAnnotationClass);
                    if (annotation != null) {
                        if (isHookAnnotation(annotation)) {
                            addHook(annotation, method, clazz.getValue());
                        } else if (isStepdefAnnotation(annotation)) {
                            addStepDefinition(annotation, method, clazz.getValue());
                        }
                    }
                }
            }
        }
    }

    private boolean isHookAnnotation(final Annotation annotation) {
        final Class<? extends Annotation> annotationClass = annotation.annotationType();
        return annotationClass.equals(Before.class) || annotationClass.equals(After.class);
    }

    private boolean isStepdefAnnotation(final Annotation annotation) {
        final Class<? extends Annotation> annotationClass = annotation.annotationType();
        return annotationClass.getAnnotation(StepDefAnnotation.class) != null;
    }

    private void addStepDefinition(final Annotation annotation, final Method method, final Object instance) {
        try {
            glue.addStepDefinition(new ArquillianStepDefinition(method, pattern(annotation), timeout(annotation), instance));
        } catch (DuplicateStepDefinitionException e) {
            throw e;
        } catch (Throwable e) {
            throw new CucumberException(e);
        }
    }

    private Pattern pattern(final Annotation annotation) throws Throwable {
        final Method regexpMethod = annotation.getClass().getMethod("value");
        final String regexpString = (String) Utils.invoke(annotation, regexpMethod, 0);
        return Pattern.compile(regexpString);
    }

    private int timeout(final Annotation annotation) throws Throwable {
        final Method regexpMethod = annotation.getClass().getMethod("timeout");
        return (Integer) Utils.invoke(annotation, regexpMethod, 0);
    }

    private void addHook(final Annotation annotation, final Method method, final Object instance) {
        if (annotation.annotationType().equals(Before.class)) {
            final String[] tagExpressions = ((Before) annotation).value();
            final int timeout = ((Before) annotation).timeout();
            glue.addBeforeHook(new ArquillianHookDefinition(method, tagExpressions, ((Before) annotation).order(), timeout, instance));
        } else {
            final String[] tagExpressions = ((After) annotation).value();
            final int timeout = ((After) annotation).timeout();
            glue.addAfterHook(new ArquillianHookDefinition(method, tagExpressions, ((After) annotation).order(), timeout, instance));
        }
    }

    @Override
    public void setUnreportedStepExecutor(UnreportedStepExecutor executor) {
        //Not used here yet
    }

    @Override
    public void buildWorld() {
        // no-op
    }

    @Override
    public void disposeWorld() {
        // no-op
    }

    @Override
    public String getSnippet(final Step step) {
        return snippetGenerator.getSnippet(step);
    }
}
