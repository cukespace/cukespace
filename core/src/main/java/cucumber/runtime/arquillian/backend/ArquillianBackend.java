package cucumber.runtime.arquillian.backend;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.ObjectFactory;
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
import cucumber.runtime.arquillian.container.ContextualObjectFactoryBase;
import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
import cucumber.runtime.java.JavaBackend;
import cucumber.runtime.java.StepDefAnnotation;
import cucumber.runtime.snippets.FunctionNameGenerator;
import cucumber.runtime.snippets.Snippet;
import cucumber.runtime.snippets.SnippetGenerator;
import gherkin.formatter.model.Step;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static cucumber.runtime.arquillian.shared.ClassLoaders.load;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

// patched to use the resource loader defined by this extension
// the best would probably to update cucumber-core to handle
// completely listed feature/steps (glue) classes/resources
public class ArquillianBackend extends JavaBackend implements Backend {
    protected enum GlueType {
        JAVA, SCALA, UNKNOWN
    }

    private SnippetGenerator snippetGenerator;
    private final Collection<Class<?>> glues = new ArrayList<Class<?>>();
    private Glue glue;
    private GlueType glueType = GlueType.UNKNOWN;
    private ObjectFactory objectFactory;
    private Class<?> testClass;

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
        this.objectFactory = defaultObjectFactory(null, null);
    }

    public ArquillianBackend(final Collection<Class<?>> classes, final Class<?> clazz, final Object testInstance, final String objectFactory) {
        this();
        this.glues.addAll(classes);
        this.testClass = clazz;
        try {
            this.objectFactory = objectFactory == null ?
                    defaultObjectFactory(clazz, testInstance) :
                    wrapObjectFactory(clazz, testInstance,
                            ObjectFactory.class.cast(Thread.currentThread().getContextClassLoader()
                                    .loadClass(objectFactory.trim()).getConstructor().newInstance()));
        } catch (final InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    // ensure test class is used as glue
    private ObjectFactory wrapObjectFactory(final Class<?> clazz, final Object testInstance, final ObjectFactory cast) {
        return new ObjectFactory() {
            @Override
            public void start() {
                cast.start();
            }

            @Override
            public void stop() {
                cast.stop();
            }

            @Override
            public boolean addClass(final Class<?> glueClass) {
                return glueClass == clazz || cast.addClass(glueClass);
            }

            @Override
            public <T> T getInstance(final Class<T> glueClass) {
                return glueClass == clazz ? glueClass.cast(testInstance) : cast.getInstance(glueClass);
            }
        };
    }

    // plain newInstance()
    private ObjectFactory defaultObjectFactory(final Class<?> clazz, final Object testInstance) {
        return new ContextualObjectFactoryBase() {
            private final Map<Class<?>, Object> instances = new HashMap<>();

            {
                // yes before start cause otherwise we'll duplicate the instance and not behave properly
                instances.put(clazz, testInstance);
            }

            @Override
            public void stop() {
                instances.clear();
            }

            @Override
            public boolean addClass(final Class<?> clazz) {
                return getInstance(clazz) != null;
            }

            @Override
            public <T> T getInstance(final Class<T> type) {
                T instance = type.cast(instances.get(type));
                if (instance == null) {
                    try {
                        final Constructor<T> constructor = type.getConstructor();
                        instance = constructor.newInstance();
                        instances.put(type, instance);
                        return instance;
                    } catch (final Exception e) {
                        throw new CucumberException(String.format("Failed to instantiate %s", type), e);
                    }
                }
                return instance;
            }
        };
    }

    @Override
    public void loadGlue(final Glue glue, final List<String> gluePaths) {
        super.loadGlue(glue, Collections.<String>emptyList());
        this.glue = glue;
        scan(); // dedicated scanning
    }

    private void initInstances() {
        for (final Class<?> glueClass : glues) {
            try {
                initLambda(CucumberLifecycle.enrich(objectFactory.getInstance(glueClass)));
            } catch (final Exception e) {
                throw new IllegalArgumentException("Can't instantiate " + glueClass.getName(), e);
            }
        }
        if (testClass != null) {
            initLambda(CucumberLifecycle.enrich(objectFactory.getInstance(testClass)));
        }
    }

    private void initLambda(final Object instance) {
        beforeCreate();
        try {
            if (Lambda.class.isInstance(instance)) {
                Lambda.class.cast(instance).define();
            }
        } finally {
            afterCreate();
        }
    }

    private void scan() {
        for (final Collection<? extends Class<?>> list : asList(glues, singletonList(testClass))) {
            for (final Class<?> clazz : list) {
                if (clazz == null) { // testclass can be null
                    continue;
                }

                if (readFromJava(clazz)) {
                    glueType = GlueType.JAVA;
                }
                if (readFromScalaDsl(objectFactory.getInstance(clazz)) && glueType != GlueType.JAVA) {
                    glueType = GlueType.SCALA;
                }
            }
        }
    }

    private boolean readFromJava(final Class<?> clazz) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
            for (final Class<? extends Annotation> cucumberAnnotationClass : CucumberLifecycle.cucumberAnnotations()) {
                final Annotation annotation = method.getAnnotation(cucumberAnnotationClass);
                if (annotation != null) {
                    if (isHookAnnotation(annotation)) {
                        addHook(annotation, method, objectFactory.getInstance(clazz));
                        found = true;
                    } else if (isStepdefAnnotation(annotation)) {
                        addStepDefinition(annotation, method, objectFactory.getInstance(clazz));
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
        objectFactory.start();
        initInstances();
    }

    @Override
    public void disposeWorld() {
        objectFactory.stop();
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
