package cucumber.runtime.arquillian.feature;

import com.thoughtworks.selenium.DefaultSelenium;
import cucumber.runtime.arquillian.ArquillianCucumber;
import cucumber.runtime.arquillian.api.Features;
import cucumber.runtime.arquillian.api.Glues;
import cucumber.runtime.arquillian.controller.BellyController;
import cucumber.runtime.arquillian.domain.Belly;
import cucumber.runtime.arquillian.feature.glue.CukeBellyGlue;
import cucumber.runtime.arquillian.producer.FacesContextProducer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import java.io.File;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

@Glues(CukeBellyGlue.class)
@Features("cucumber/runtime/arquillian/feature/cukes-in-belly.feature:3")
@RunWith(ArquillianCucumber.class)
public class CukesInBellyFunctionalTest {
    @Deployment(testable = false)
    public static Archive<?> createDeployment() {
        return create(WebArchive.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource(new StringAsset("<faces-config version=\"2.0\"/>"), "faces-config.xml")
            .addAsWebResource(new File("src/main/webapp/belly.xhtml"), "belly.xhtml")
            .addClass(Belly.class)
            .addClass(BellyController.class)
            .addClass(FacesContextProducer.class);
    }

    // just because drone extension check test class only for injections. Without any it doesn't start the context
    // will be fixed with a next version
    @Drone
    private DefaultSelenium selenium;
}
