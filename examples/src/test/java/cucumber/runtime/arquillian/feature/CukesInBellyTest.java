package cucumber.runtime.arquillian.feature;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import cucumber.runtime.arquillian.domain.Belly;
import cucumber.runtime.arquillian.glue.server.BellySteps;
import cucumber.runtime.arquillian.junit.Cucumber;
import cucumber.runtime.arquillian.junit.ServerSideTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.experimental.categories.Category;

@Category(ServerSideTest.class)
public class CukesInBellyTest extends Cucumber {
    @Deployment
    public static Archive<?> createDeployment() {
        return create(WebArchive.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsResource("cucumber/runtime/arquillian/feature/cukes.feature")
            .addClass(Belly.class)
            .addClass(BellySteps.class)
            .addClass(CukesInBellyTest.class);
    }
    
    @Override
    protected void initializeRuntimeOptions() {
        runtimeOptions.featurePaths.add("classpath:cucumber/runtime/arquillian/feature");
        runtimeOptions.glue.add("classpath:cucumber/runtime/arquillian/glue/server");
    }
}
