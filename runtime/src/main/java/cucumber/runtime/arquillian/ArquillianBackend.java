package cucumber.runtime.arquillian;

import java.util.List;

import gherkin.formatter.model.Step;
import cucumber.runtime.Backend;
import cucumber.runtime.Glue;
import cucumber.runtime.UnreportedStepExecutor;


/**
 * Arquillian back-end for Cucumber.
 */
public class ArquillianBackend implements Backend
{
    /**
     * Initializes a new instance of the ArquillianBackend class.
     */
    public ArquillianBackend()
    {
        // TODO Auto-generated constructor stub
        
    } // ArquillianBackend
    
    
    /**
     * @see cucumber.runtime.Backend#loadGlue(cucumber.runtime.Glue, java.util.List)
     */
    @Override
    public void loadGlue( Glue glue, List<String> gluePaths )
    {
        // TODO Auto-generated method stub
        
    } // loadGlue
    
    
    /**
     * @see cucumber.runtime.Backend#setUnreportedStepExecutor(cucumber.runtime.UnreportedStepExecutor)
     */
    @Override
    public void setUnreportedStepExecutor( UnreportedStepExecutor executor )
    {
        // TODO Auto-generated method stub
        
    } // setUnreportedStepExecutor
    
    
    /**
     * @see cucumber.runtime.Backend#buildWorld()
     */
    @Override
    public void buildWorld()
    {
        // TODO Auto-generated method stub
        
    } // buildWorld
    
    
    /**
     * @see cucumber.runtime.Backend#disposeWorld()
     */
    @Override
    public void disposeWorld()
    {
        // TODO Auto-generated method stub
        
    } // disposeWorld
    
    
    /**
     * @see cucumber.runtime.Backend#getSnippet(gherkin.formatter.model.Step)
     */
    @Override
    public String getSnippet( Step step )
    {
        // TODO Auto-generated method stub
        return null;
        
    } // getSnippet
    
    
} // class ArquillianBackend
