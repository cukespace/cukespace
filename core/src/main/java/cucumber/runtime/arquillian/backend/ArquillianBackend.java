package cucumber.runtime.arquillian.backend;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

// patched to use the resource loader defined by this extension
// the best would probably to update cucumber-core to handle
// completely listed feature/steps (glue) classes/resources
public class ArquillianBackend implements Backend {
    private final SnippetGenerator snippetGenerator = new SnippetGenerator(new ArquillianSnippet());
    private final Object instance;
    private final Class<?> clazz;
    private Glue glue;

    public ArquillianBackend(final Class<?> testClass, final Object testInstance) {
        this.instance = testInstance;
        this.clazz = testClass;
    }

    @Override
    public void loadGlue(final Glue glue, final List<String> gluePaths) {
        this.glue = glue;

        // dedicated scanning
        for (final Method method : clazz.getMethods()) {
            for (final Class<? extends Annotation> cucumberAnnotationClass : CucumberLifecycle.cucumberAnnotations()) {
                final Annotation annotation = method.getAnnotation(cucumberAnnotationClass);
                if (annotation != null) {
                    if (isHookAnnotation(annotation)) {
                        addHook(annotation, method);
                    } else if (isStepdefAnnotation(annotation)) {
                        addStepDefinition(annotation, method);
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

    void addStepDefinition(final Annotation annotation, final Method method) {
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

    void addHook(Annotation annotation, Method method) {
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
