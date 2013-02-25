Cukes in Space!
===============

Cukes in Space! allows you to deploy and run Cucumber features in the
application server of your choice using the Arquillian test framework.

## Supported application servers:

The following application servers are supported. The artifact
```cukespace-core``` is required for all servers, and additional dependencies
have been listed for each.

# Quickstart

This quickstart assumes you're already very familiar with [Arquillian][] and
[Cucumber-JVM][], and that you've set up your Maven.

[Arquillian]: http://www.arquillian.org/
[Cucumber-JVM]: http://www.github.com/cucumber/cucumber-jvm
[JBoss repository]: https://community.jboss.org/wiki/MavenGettingStarted-Users

## Installation

Before you start writing features, you'll want to pull down the source for
Cukes in Space! and install it to your local repository using the following
command:

```mvn install```

## Project Setup

You'll want at least the following dependency in your pom.xml:

```xml
<dependency>
    <groupId>com.github.cukespace</groupId>
    <artifactId>cukespace-core</artifactId>
    <version>{VERSION}</version>
    <scope>test</scope>
</dependency>
```

## Creating Features

All you have to do is to replace Arquillian runner by ```ArquillianCucumber```, create the test deployment, and
write your steps in your test class:

```java
package my.features;

import cucumber.runtime.arquillian.junit.Cucumber;
import my.features.domain.Belly;
import my.features.glue.BellySteps;

@RunWith(ArquillianCucumber.class)
public class CukesInBellyTest {
    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsResource("my/features/cukes.feature")
            .addClass(Belly.class)
            .addClass(BellySteps.class)
            .addClass(CukesInBellyFeature.class);
    }
    
    @EJB
    private CukeService service;

    @Inject
    private CukeLocator cukeLocator;

    @When("^I persist my cuke$")
    public void persistCuke() {
        this.service.persist(this.cukeLocator.findCuke());
    }
}
```

Arquillian will then package up all the necessary dependencies along with your
test deployment and execute the feature in the application server. Your step
definitions will also be serviced by Arquillian's awesome test enrichers, so
your steps will have access to any resource supported by the Arquillian
container you choose to use:

```java
// clip
@EJB
private CukeService service;

@Resource
private Connection connection;

@PersistenceContext
private EntityManager entityManager;

@Inject
private CukeLocator cukeLocator;

@When("^I persist my cuke$")
public void persistCuke() {
    this.entityManager.persist(this.cukeLocator.findCuke());
}
// clip
``` 

### Functional UI Testing with Arquillian Drone

[This guide][] will help you get started with using the Arquillian Drone
extension for functional testing.

[This guide]: http://arquillian.org/guides/functional_testing_using_drone/

To create features for functional UI testing, you first want to add all
necessary Drone dependencies to your project's POM, then mark your deployment
as untestable and inject a webdriver:

```java
// clip
@Drone
DefaultSelenium browser;

@Deployment(testable = false)
public static Archive<?> createDeployment() {
    return ShrinkWrap.create(WebArchive.class)
        .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
        .addAsWebInfResource(new StringAsset("<faces-config version=\"2.0\"/>"), "faces-config.xml")
        .addAsWebResource(new File("src/main/webapp/belly.xhtml"), "belly.xhtml")
        .addClass(Belly.class)
        .addClass(BellyController.class);
}
// clip
```

You can then access your Drone from any step definition.

```java
public class IrresistibleButtonSteps {
    @Drone
    private DefaultSelenium browser;
    
    @When("^I click on an irresistible button$")
    public void click() {
        this.browser.click("id=irresistible-button");
    }
}
```

Be sure to remember to inject the webdriver into your test fixture, or you
won't be able to inject it into any of your step definitions. You'll know when
you've forgotten because you'll get the following error:

```
java.lang.IllegalArgumentException: Drone Test context should not be null
```

### Externalize some common features/steps
#### Features

If you want to reuse some feature in multiple test you can specify it through @Features:

```java
@Features("org/foo/bar/scenarii.feature") // can support multiple features too
@RunWith(ArquillianCucumber.class)
public class MyFeatureTest {
    ....
}
```

#### Steps

If you want to reuse some step classes you can using the annotation @Glues:

```java
@Glues(MySteps.class) // can support multiple steps classes too
@RunWith(ArquillianCucumber.class)
public class MyFeatureTest {
    ....
}
```
