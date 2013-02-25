package cucumber.runtime.arquillian.feature;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Features {
    private Features() {
        // no-op
    }

    public static String featurePath(final Class<?> javaClass) {
        return javaClass.getPackage().getName().replace('.', '/')
                + '/' + createClassNameSubPackage(javaClass.getSimpleName()) + ".feature";
    }

    public static Map<String, URL> createFeatureMap(final Class<?> javaClass, final ClassLoader loader) {
        final Map<String, URL> featureUrls = new HashMap<String, URL>();
        for (final String path : findFeatures(javaClass)) {
            final URL url = loader.getResource(path);
            if (url != null) {
                featureUrls.put(path, url);
            }
        }
        return featureUrls;
    }

    public static Collection<String> findFeatures(final Class<?> javaClass) {
        final Collection<String> featureUrls = new ArrayList<String>();
        final String featurePath = Features.featurePath(javaClass);
        featureUrls.add(featurePath);

        final cucumber.runtime.arquillian.api.Features additionalFeaturesAnn = javaClass.getAnnotation(cucumber.runtime.arquillian.api.Features.class);
        if (additionalFeaturesAnn != null) {
            Collections.addAll(featureUrls, additionalFeaturesAnn.value());
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
