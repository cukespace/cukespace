package cucumber.runtime.arquillian.feature;

import cucumber.api.junit.Cucumber;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class Features {
    private static final Logger LOGGER = Logger.getLogger(Features.class.getName());

    private static final String FEATURE_WILDCARD = "*.feature";

    private Features() {
        // no-op
    }

    public static String featurePath(final Class<?> javaClass) {
        return javaClass.getPackage().getName().replace('.', '/')
                + '/' + createClassNameSubPackage(javaClass.getSimpleName()) + ".feature";
    }

    public static Map<String, Collection<URL>> createFeatureMap(final Class<?> javaClass, final ClassLoader loader) {
        final Map<String, Collection<URL>> featureUrls = new HashMap<String, Collection<URL>>();

        for (final String path : findFeatures(javaClass)) {
            final Collection<URL> list = new ArrayList<URL>();

            { // from classpath
                final URL url = loader.getResource(path);
                if (url != null) {
                    list.add(url);
                    featureUrls.put(path, list);
                    continue;
                }
            }

            { // from filesystem
                final File file = new File(path);
                if (file.exists()) {
                    try {
                        list.add(file.toURI().toURL());
                        featureUrls.put(path, list);
                        continue;
                    } catch (final MalformedURLException e) {
                        // no-op
                    }
                }
            }

            // else try some special tricks
            // special wildcard extension
            if (path.endsWith(FEATURE_WILDCARD)) {
                final String newPath = path.substring(0, path.length() - FEATURE_WILDCARD.length());
                featureUrls.put(newPath, list);

                final File f = new File(newPath);
                if (f.exists() && f.isDirectory()) {
                    final File[] children = f.listFiles();
                    if (children != null) {
                        for (final File c : children) {
                            if (c.exists()) {
                                try {
                                    list.add(c.toURI().toURL());
                                } catch (final MalformedURLException e) {
                                    // no-op
                                }
                            }
                        }
                    }
                }
            } else {
                // not found
                LOGGER.warning("Can't find feature(s) " + path);
            }
        }

        return featureUrls;
    }

    public static Collection<String> findFeatures(final Class<?> javaClass) {
        final Collection<String> featureUrls = new ArrayList<String>();

        { // convention
            final String featurePath = Features.featurePath(javaClass);
            featureUrls.add(featurePath);
        }

        { // our API
            final cucumber.runtime.arquillian.api.Features additionalFeaturesAnn = javaClass.getAnnotation(cucumber.runtime.arquillian.api.Features.class);
            if (additionalFeaturesAnn != null) {
                Collections.addAll(featureUrls, additionalFeaturesAnn.value());
            }
        }

        { // cucumber-junit API
            final Cucumber.Options annotation = javaClass.getAnnotation(Cucumber.Options.class);
            if (annotation != null && annotation.features() != null) {
                Collections.addAll(featureUrls, annotation.features());
            }
        }

        return featureUrls;
    }

    private static String createClassNameSubPackage(final String name) {
        String result = name;
        if (result.endsWith("Test")) {
            result = result.substring(0, result.length() - "Test".length());
        } else if (result.endsWith("IT")) {
            result = result.substring(0, result.length() - "IT".length());
        }

        if (result.length() == 1) {
            return result;
        }

        return Character.toLowerCase(result.charAt(0)) + replaceUpperCaseWithADashAndLowercase(result.substring(1));
    }

    private static String replaceUpperCaseWithADashAndLowercase(final String substring) {
        final StringBuilder builder = new StringBuilder();
        for (final char c : substring.toCharArray()) {
            if (!Character.isUpperCase(c)) {
                builder.append(c);
            } else {
                builder.append('-').append(Character.toLowerCase(c));
            }
        }

        final String s = builder.toString();
        if (s.endsWith("-")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

}
