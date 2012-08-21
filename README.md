Cucumber-JVM/Arquillian Integration
===================================

This project allows you to deploy and run Cucumber features into the
application server of your choice using the Arquillian test framework.

## Supported application servers:

The following application servers are supported with Cucumber-JVM/Arquillian.
The artifact ```cucumber-arquillian-core``` is required for all servers, and
additional dependencies have been listed for each.

| Server      | Additional Dependencies   |
|-------------|---------------------------|
| JBoss AS 7  | cucumber-arquillian-jbas7 |
| Glassfish 3 |                           |

# Quickstart

This quickstart assumes you're already very familiar with [Arquillian](http://www.arquillian.org/)
and [Cucumber-JVM](http://www.github.com/cucumber/cucumber-jvm).

## Installation

Before you start writing features, you'll want to pull down the source for
Cucumber-JVM/Arquillian and install it to your local repository using the
following command:

```mvn install```

If you're feeling confident, you can even do it without testing:

```mvn install -DskipTests```

## Project Setup

You'll want at least the following dependency in your pom.xml:

```xml
<dependency>
    <groupId>info.cukes.runtime.arquillian</groupId>
    <artifactId>cucumber-arquillian-core</artifactId>
    <version>{VERSION}</version>
    <scope>test</scope>
</dependency>
```

You'll also want to add dependencies for the application server you wish to
test against. Here's an example dependency for JBoss AS 7:

```xml
<dependency>
    <groupId>info.cukes.runtime.arquillian</groupId>
    <artifactId>cucumber-arquillian-jbas7</artifactId>
    <version>{VERSION}</version>
    <scope>test</scope>
</dependency>
```

## Creating Features

All you have to do is extend ```Cucumber```, create the test deployment, and
tailor the Cucumber runtime options:

```java
package my.features;

import cucumber.runtime.arquillian.junit.Cucumber;
import my.features.domain.Belly;
import my.features.glue.BellySteps;

public class CukesInBellyFeature extends Cucumber {
    
    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsResource("my/features/cukes.feature")
            .addClass(Belly.class)
            .addClass(BellySteps.class)
            .addClass(CukesInBellyFeature.class);
    }
    
    @Before
    public void initializeRuntimeOptions() {
        RuntimeOptions runtimeOptions = this.getRuntimeOptions();
        runtimeOptions.featurePaths.add("classpath:my/features");
        runtimeOptions.glue.add("classpath:my/features/glue");
    }
}
```

Arquillian will then package up all the necessary dependencies along with your
test deployment and execute the feature in the application server.
