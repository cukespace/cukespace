package cucumber.runtime.arquillian.backend;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.runtime.Backend;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.CucumberException;
import cucumber.runtime.DuplicateStepDefinitionException;
import cucumber.runtime.Glue;
import cucumber.runtime.HookDefinition;
import cucumber.runtime.StepDefinition;
import cucumber.runtime.UnreportedStepExecutor;
import cucumber.runtime.Utils;
import cucumber.runtime.arquillian.api.Lambda;
import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
import cucumber.runtime.java.JavaBackend;
import cucumber.runtime.java.StepDefAnnotation;
import cucumber.runtime.snippets.FunctionNameGenerator;
import cucumber.runtime.snippets.Snippet;
import cucumber.runtime.snippets.SnippetGenerator;
import gherkin.formatter.model.Step;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static cucumber.runtime.arquillian.shared.ClassLoaders.load;

// patched to use the resource loader defined by this extension
// the best would probably to update cucumber-core to handle
// completely listed feature/steps (glue) classes/resources
public class ArquillianBackend extends JavaBackend implements Backend {
    protected static enum GlueType {
        JAVA, SCALA, UNKNOWN
    }

    private SnippetGenerator snippetGenerator;
    private final Map<Class<?>, Object> instances = new HashMap<Class<?>, Object>();
    private final Collection<Class<?>> glues = new ArrayList<Class<?>>();
    private Glue glue;
    private GlueType glueType = GlueType.UNKNOWN;

    public ArquillianBackend() { // no-op constructor but we need to be JavaBackend for java8 integration
        super(null, new ClassFinder() {
            private final ClassLoader loader = Thread.currentThread().getContextClassLoader();

            @Override
            public <T> Collection<Class<? extends T>> getDescendants(final Class<T> parentType, final String packageName) {
                return Collections.emptyList();
            }

            @Override
            public <T> Class<? extends T> loadClass(final String s) throws ClassNotFoundException {
                return (Class<? extends T>) loader.loadClass(s);
            }
        });
    }

    public ArquillianBackend(final Collection<Class<?>> classes, final Class<?> clazz, final Object testInstance) {
        this();
        instances.put(clazz, testInstance);
        glues.addAll(classes);
    }

    @Override
    public void loadGlue(final Glue glue, final List<String> gluePaths) {
        super.loadGlue(glue, Collections.<String>emptyList());
        this.glue = glue;
        for (final Object i : instances.values()) {
            initLambda(i);
        }
        initInstances();
        scan(); // dedicated scanning
    }

    private void initInstances() {
        for (final Class<?> glueClass : glues) {
            final Object instance;
            try {
                instance = initLambda(glueClass.newInstance());
            } catch (final Exception e) {
                throw new IllegalArgumentException("Can't instantiate " + glueClass.getName(), e);
            }

            instances.put(glueClass, CucumberLifecycle.enrich(instance));
        }
    }

    private Object initLambda(final Object instance) {
        beforeCreate();
        try {
            if (Lambda.class.isInstance(instance)) {
                Lambda.class.cast(instance).define();
            }
            return instance;
        } finally {
            afterCreate();
        }
    }

    private void scan() {
        for (final Map.Entry<Class<?>, Object> clazz : instances.entrySet()) {
            if (readFromJava(clazz)) {
                glueType = GlueType.JAVA;
            }
            if (readFromScalaDsl(clazz.getValue()) && glueType != GlueType.JAVA) {
                glueType = GlueType.SCALA;
            }
        }
    }

    private boolean readFromJava(Map.Entry<Class<?>, Object> clazz) {
        boolean found = false;
        for (final Method method : clazz.getKey().getMethods()) {
            for (final Class<? extends Annotation> cucumberAnnotationClass : CucumberLifecycle.cucumberAnnotations()) {
                final Annotation annotation = method.getAnnotation(cucumberAnnotationClass);
                if (annotation != null) {
                    if (isHookAnnotation(annotation)) {
                        addHook(annotation, method, clazz.getValue());
                        found = true;
                    } else if (isStepdefAnnotation(annotation)) {
                        addStepDefinition(annotation, method, clazz.getValue());
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    private boolean readFromScalaDsl(final Object instance) {
        try {
            // ensure scala module is activated
            load("cucumber.api.scala.ScalaDsl");

            // read info directly {@see cucumber.api.scala.ScalaDsl}
            final Class<?> clazz = instance.getClass();

            final Collection<StepDefinition> stepDefinitions = readField(clazz, "stepDefinitions", instance, StepDefinition.class);
            for (final StepDefinition sd : stepDefinitions) {
                glue.addStepDefinition(StepDefinition.class.cast(sd));
            }

            final Collection<HookDefinition> beforeHooks = readField(clazz, "beforeHooks", instance, HookDefinition.class);
            for (final HookDefinition sd : beforeHooks) {
                glue.addBeforeHook(HookDefinition.class.cast(sd));
            }

            final Collection<HookDefinition> afterHooks = readField(clazz, "afterHooks", instance, HookDefinition.class);
            for (final HookDefinition sd : afterHooks) {
                glue.addAfterHook(HookDefinition.class.cast(sd));
            }

            return stepDefinitions.size() + beforeHooks.size() + afterHooks.size() > 0;
        } catch (final Exception e) {
            return false;
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

    private long timeout(final Annotation annotation) throws Throwable {
        final Method regexpMethod = annotation.getClass().getMethod("timeout");
        return (Long) Utils.invoke(annotation, regexpMethod, 0);
    }

    private void addHook(final Annotation annotation, final Method method, final Object instance) {
        if (annotation.annotationType().equals(Before.class)) {
            final String[] tagExpressions = ((Before) annotation).value();
            final long timeout = ((Before) annotation).timeout();
            glue.addBeforeHook(new ArquillianHookDefinition(method, tagExpressions, ((Before) annotation).order(), timeout, instance));
        } else {
            final String[] tagExpressions = ((After) annotation).value();
            final long timeout = ((After) annotation).timeout();
            glue.addAfterHook(new ArquillianHookDefinition(method, tagExpressions, ((After) annotation).order(), timeout, instance));
        }
    }

    @Override
    public void setUnreportedStepExecutor(UnreportedStepExecutor executor) {
        //Not used here yet
    }

    public void beforeCreate() {
        INSTANCE.set(this);
    }

    public void afterCreate() {
        INSTANCE.remove();
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
    public String getSnippet(final Step step, final FunctionNameGenerator nameGenerator) {
        if (snippetGenerator == null) { // leaving a double if ATM if we need to add other language support
            if (GlueType.SCALA.equals(glueType)) {
                try {
                    snippetGenerator = new SnippetGenerator(Snippet.class.cast(load("cucumber.runtime.scala.ScalaSnippetGenerator").newInstance()));
                } catch (final Exception e) {
                    // let use the default
                }
            }
        }

        if (snippetGenerator == null) { // JAVA is the default too
            snippetGenerator = new SnippetGenerator(new ArquillianSnippet());
        }

        return snippetGenerator.getSnippet(step, nameGenerator);
    }

    private static <T> Collection<T> readField(final Class<?> clazz, final String field, final Object instance, final Class<T> cast) throws Exception {
        final Field f = clazz.getDeclaredField(field);
        f.setAccessible(true);
        final Object o = f.get(instance);

        final Class<?> arrayBuffer = load("scala.collection.mutable.ArrayBuffer");
        if (arrayBuffer.isInstance(o)) {
            final Object[] array = Object[].class.cast(arrayBuffer.getDeclaredMethod("array").invoke(o));
            final Collection<T> result = new ArrayList<T>(array.length);
            for (final Object i : array) {
                if (cast.isInstance(i)) {
                    result.add(cast.cast(i));
                }
            }
            return result;
        }

        throw new IllegalArgumentException("expected an ArrayBuffer and got " + o);
    }
}
