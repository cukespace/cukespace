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
        // intentionally empty
        
    } // ArquillianBackend
    
    
    @Override
    public void loadGlue( Glue glue, List<String> gluePaths )
    {
        // TODO Auto-generated method stub
        
    } // loadGlue
    
    
    @Override
    public void setUnreportedStepExecutor( UnreportedStepExecutor executor )
    {
        // TODO Auto-generated method stub
        
    } // setUnreportedStepExecutor
    
    
    @Override
    public void buildWorld()
    {
        // TODO Auto-generated method stub
        
    } // buildWorld
    
    
    @Override
    public void disposeWorld()
    {
        // TODO Auto-generated method stub
        
    } // disposeWorld
    
    
    @Override
    public String getSnippet( Step step )
    {
        // TODO Auto-generated method stub
        return null;
        
    } // getSnippet
    
    
} // class ArquillianBackend
