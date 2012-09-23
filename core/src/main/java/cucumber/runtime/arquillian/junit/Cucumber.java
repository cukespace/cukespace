package cucumber.runtime.arquillian.junit;

import java.util.List;
import java.util.Properties;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.runner.RunWith;

import cucumber.io.MultiLoader;
import cucumber.io.ResourceLoader;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;

@RunWith(Arquillian.class)
public abstract class Cucumber {
    
    private final RuntimeOptions runtimeOptions;
    
    public Cucumber() {
        this.runtimeOptions = new RuntimeOptions(new Properties(), "-m");
        this.runtimeOptions.strict = true;
    }
    
    public RuntimeOptions getRuntimeOptions() {
        return this.runtimeOptions;
    }
    
    @Test
    public void runFeatures() throws Exception {
        this.initializeRuntimeOptions();
        Runtime runtime = this.createRuntime();
        runtime.run();
        
        List<Throwable> errors = runtime.getErrors();
        
        try {
            if (!errors.isEmpty()) {
                throw new MultipleFailureException(errors);
            }
        } finally {
            this.destroyRuntime(runtime);
        }
    }
    
    protected ResourceLoader createResourceLoader() {
        return new MultiLoader(this.getClassLoader());
    }
    
    protected Runtime createRuntime() {
        Runtime runtime = new Runtime(
            this.createResourceLoader(),
            this.getClassLoader(),
            this.getRuntimeOptions()
        );
        
        return runtime;
    }
    
    protected void destroyRuntime(Runtime runtime) {
        
    }
    
    protected ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }
    
    protected abstract void initializeRuntimeOptions();
}
