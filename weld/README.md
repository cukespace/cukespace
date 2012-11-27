# Cukes in Space! Weld Extension

This extension allows all client-side glue classes and step definitions to use
CDI annotations for dependency injection.

## Usage

All classes that require enrichment by Arquillian must be annotated by
`cucumber.runtime.arquillian.weld.Enriched`. This will inject Arquillian
resources into the new instance right before the `@PostConstruct` callback
executes.

```java
@Enriched
public class Hello {
    @Drone
    private DefaultSelenium browser;
    @ArquillianResource
    private URL deploymentUrl;
    @Inject
    private World world;

    @PostConstruct
    public void verifyInjection() {
        assert browser != null : "Field #browser is null";
        assert deploymentUrl != null : "Field #deploymentUrl is null";
    }
}

@Enriched
public class World {
    @Drone
    private DefaultSelenium browser;

    @PostConstruct
    public void verifyInjection() {
        assert browser != null : "Field #browser is null";
    }
}
```

Without the `@Enriched` annotation, both the `browser` and `deploymentUrl`
fields in each class will be null. Try the above code with and without the
`@Enriched` annotation to see what happens.

## Caveats

This extension is intended to be used with features that are tested from the
client. Running server-side features with this extension is not supported and
may result in strange behavior if the server provides CDI services.

See the main README for details on how to run client-side features.
