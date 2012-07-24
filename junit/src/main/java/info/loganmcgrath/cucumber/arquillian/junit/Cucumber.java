package info.loganmcgrath.cucumber.arquillian.junit;

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


/**
 * JUnit fixture for running Cucumber features.
 */
@RunWith( Arquillian.class )
public abstract class Cucumber
{
    /**
     * The runtime options.
     */
    private final RuntimeOptions runtimeOptions;
    
    
    /**
     * Initializes a new instance of the Cucumber class.
     */
    public Cucumber()
    {
        // don't use System.getProperties() because developers may not have
        // direct control over the server environment
        this.runtimeOptions = new RuntimeOptions( new Properties(), "-m" );
        this.runtimeOptions.strict = true;
        
    } // Cucumber
    
    
    /**
     * Gets the runtime options.
     * 
     * @return The runtime options.
     */
    public RuntimeOptions getRuntimeOptions()
    {
        return this.runtimeOptions;
        
    } // getRuntimeOptions
    
    
    /**
     * Runs the Cucumber features.
     * 
     * @throws Exception Thrown if there are errors.
     */
    @Test
    public void runFeatures() throws Exception
    {
        Runtime runtime = this.createRuntime();
        
        runtime.run();
        
        List<Throwable> errors = runtime.getErrors();
        
        try
        {
            if( !errors.isEmpty() )
            {
                throw new MultipleFailureException( errors );
            }
        }
        finally
        {
            this.destroyRuntime( runtime );
        }
        
    } // runFeatures
    
    
    /**
     * Creates a resource loader.
     * 
     * @return The new resource loader.
     */
    protected ResourceLoader createResourceLoader()
    {
        return new MultiLoader( this.getClassLoader() );
        
    } // createResourceLoader
    
    
    /**
     * Create the runtime.
     * 
     * @return The new runtime.
     */
    protected Runtime createRuntime()
    {
        Runtime runtime = new Runtime(
            this.createResourceLoader(),
            this.getClassLoader(),
            this.getRuntimeOptions()
        );
        
        return runtime;
        
    } // createRuntime
    
    
    /**
     * Destroys a runtime.
     * 
     * @param runtime The runtime to destroy.
     */
    protected void destroyRuntime( Runtime runtime )
    {
        // intentionally empty
        
    } // destroyRuntime
    
    
    /**
     * Gets the class loader to use.
     * 
     * @return The class loader.
     */
    protected ClassLoader getClassLoader()
    {
        return this.getClass().getClassLoader();
        
    } // getClassLoader
    
    
} // class Cucumber
