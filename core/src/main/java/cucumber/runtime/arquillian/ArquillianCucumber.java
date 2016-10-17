package cucumber.runtime.arquillian;

import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.arquillian.runner.BaseCukeSpace;
import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.junit.JUnitOptions;
import cucumber.runtime.junit.JUnitReporter;
import cucumber.runtime.model.CucumberFeature;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class ArquillianCucumber extends Arquillian {
    private static final String RUN_CUCUMBER_MTD = "performInternalCucumberOperations";

    private List<FrameworkMethod> methods;
    private final BaseCukeSpace<JUnitReporter, RunNotifier> delegate = new BaseCukeSpace<JUnitReporter, RunNotifier>() {
        @Override
        protected Class<?> getTestedClass() {
            return getTestClass().getJavaClass();
        }

        @Override
        protected Exception newMultipleFailureException(final List<Throwable> errors) {
            return new MultipleFailureException(errors);
        }

        @Override
        protected void runFeature(final CucumberFeature feature, final BaseCukeSpace.CucumberRuntime cucumberRuntime,
                                  final JUnitReporter reporter, final RunNotifier runNotifier) throws Throwable {
            new FeatureRunner(feature, cucumberRuntime, reporter).run(runNotifier);
        }

        @Override
        protected JUnitReporter getReporter(final Reporter delegate, final Formatter formatter, final RuntimeOptions runtimeOptions) {
            return new JUnitReporter(
                    delegate, formatter,
                    runtimeOptions.isStrict(), new JUnitOptions(runtimeOptions.getJunitOptions()));
        }
    };

    public ArquillianCucumber(final Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Description describeChild(final FrameworkMethod method) {
        if (!Boolean.getBoolean("cukespace.runner.standard-describe")
                && InstanceControlledFrameworkMethod.class.isInstance(method)) {
            return Description.createTestDescription(
                    InstanceControlledFrameworkMethod.class.cast(method).getOriginalClass(),
                    ArquillianCucumber.RUN_CUCUMBER_MTD,
                    method.getAnnotations());
        }
        return super.describeChild(method);
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        if (methods != null) {
            return methods;
        }

        methods = new LinkedList<FrameworkMethod>();

        // run @Test methods
        for (final FrameworkMethod each : ArquillianCucumber.super.computeTestMethods()) {
            methods.add(each);
        }

        try { // run cucumber, this looks like a hack but that's to keep @Before/@After/... hooks behavior
            final Method runCucumber = BaseCukeSpace.class.getDeclaredMethod(RUN_CUCUMBER_MTD, Object.class, Object.class);
            methods.add(new InstanceControlledFrameworkMethod(this, getTestClass().getJavaClass(), runCucumber));
        } catch (final NoSuchMethodException e) {
            // no-op: will not accur...if so this exception is not your biggest issue
        }

        return methods;
    }

    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        if (InstanceControlledFrameworkMethod.class.isInstance(method)) {
            InstanceControlledFrameworkMethod.class.cast(method).setNotifier(notifier);
        }
        super.runChild(method, notifier);
    }

    public static class InstanceControlledFrameworkMethod extends FrameworkMethod {
        private final ArquillianCucumber instance;
        private final Class<?> originalClass;
        private RunNotifier notifier;

        private InstanceControlledFrameworkMethod(final ArquillianCucumber runner, final Class<?> originalClass, final Method runCucumber) {
            super(runCucumber);
            this.originalClass = originalClass;
            this.instance = runner;
        }

        @Override
        public Object invokeExplosively(final Object target, final Object... params) throws Throwable {
            instance.delegate.performInternalCucumberOperations(target, notifier == null ? new RunNotifier() : notifier);
            return null;
        }

        public Class<?> getOriginalClass() {
            return originalClass;
        }

        public void setNotifier(final RunNotifier notifier) {
            this.notifier = notifier;
        }
    }
}
