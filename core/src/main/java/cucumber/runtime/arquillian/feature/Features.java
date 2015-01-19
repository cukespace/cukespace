package cucumber.runtime.arquillian.feature;

import cucumber.api.CucumberOptions;
import cucumber.runtime.io.FileResource;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.Resource;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ZipResource;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import static cucumber.runtime.arquillian.client.IOs.dump;
import static cucumber.runtime.arquillian.client.IOs.slurp;
import static java.util.Arrays.asList;

public final class Features {
    private static final Logger LOGGER = Logger.getLogger(Features.class.getName());

    public static final String EXTENSION = ".feature";

    private Features() {
        // no-op
    }

    public static String featurePath(final Class<?> javaClass) {
        return javaClass.getPackage().getName().replace('.', '/')
                + '/' + createClassNameSubPackage(javaClass.getSimpleName()) + EXTENSION;
    }

    public static Map<String, Collection<URL>> createFeatureMap(final String tempDir, final String featureHome,
                                                                final Class<?> javaClass, final ClassLoader loader) {
        final Map<String, Collection<URL>> featureUrls = new HashMap<String, Collection<URL>>();

        final String home;
        if (featureHome != null && !featureHome.endsWith("/")) {
            home = featureHome + "/";
        } else {
            home = featureHome;
        }

        final cucumber.runtime.arquillian.api.Features additionalFeaturesAnn = javaClass.getAnnotation(cucumber.runtime.arquillian.api.Features.class);
        final Collection<ResourceLoader> customLoaders = new LinkedList<ResourceLoader>();
        if (additionalFeaturesAnn != null) {
            final Class<? extends ResourceLoader>[] userLoaders = additionalFeaturesAnn.loaders();
            for (final Class<? extends ResourceLoader> resourceLoader : userLoaders) {
                try {
                    final ResourceLoader instance = resourceLoader.newInstance();
                    customLoaders.add(instance);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("can't create a " + resourceLoader.getName(), e);
                }
            }
        }

        for (final String raw : findFeatures(javaClass)) {
            final Collection<URL> list = new ArrayList<URL>();

            int lineIdx = raw.lastIndexOf(':');
            final String path;
            final String suffix;
            if (lineIdx > 0 && lineIdx + 1 != MultiLoader.CLASSPATH_SCHEME.length()) {
                path = raw.substring(0, lineIdx);
                suffix = raw.substring(lineIdx);
            } else {
                suffix = "";
                path = raw;
            }

            final boolean directResource = path.endsWith(EXTENSION);

            if (directResource) {
                { // from classpath
                    final URL url = loader.getResource(path);
                    if (url != null) {
                        list.add(url);
                        featureUrls.put(raw + suffix, list);
                        continue;
                    }
                }

                // from filesystem
                if (urlFromFileSystem(featureUrls, list, path, path, suffix)) {
                    continue;
                }

                // from filesystem with featureHome
                if (home != null && urlFromFileSystem(featureUrls, list, path, featureHome + path, suffix)) {
                    continue;
                }

                for (final ResourceLoader instance : customLoaders) {
                    for (final Resource r : instance.resources(path.substring(0, path.length() - EXTENSION.length()), EXTENSION)) {
                        try {
                            final String feature = new String(slurp(r.getInputStream()));
                            final String featurePath = r.getPath();
                            final File featureDump = dump(tempDir, javaClass.getName() + '/' + featurePath, feature);
                            featureDump.deleteOnExit();
                            featureUrls.put(featurePath, asList(featureDump.toURI().toURL()));
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
            }

            findWithCucumberSearcher(loader, path, list);
            if (home != null) {
                findWithCucumberSearcher(loader, home + path, list);
            }

            if (!list.isEmpty()) {
                featureUrls.put(path + suffix, list);
            }
        }

        for (final ResourceLoader instance : customLoaders) {
            try {
                for (final Resource r : instance.resources(null, EXTENSION)) {
                    try {
                        final String feature = new String(slurp(r.getInputStream()));
                        final String featurePath = r.getPath();
                        final File featureDump = dump(tempDir, javaClass.getName() + '/' + featurePath, feature);
                        featureDump.deleteOnExit();
                        featureUrls.put(featurePath, asList(featureDump.toURI().toURL()));
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            } catch (final NullPointerException npe) {
                // no-op: we call it with null, don't expect miracles
            } catch (final IllegalArgumentException npe) {
                // no-op: we call it with null, don't expect miracles
            }
        }

        LOGGER.fine("Features: " + featureUrls);

        return featureUrls;
    }

    private static boolean urlFromFileSystem(final Map<String, Collection<URL>> featureUrls, final Collection<URL> list,
                                             final String path, final String filePath, final String keySuffix) {
        final File file = new File(filePath);
        if (file.exists() && !file.isDirectory()) {
            try {
                list.add(file.toURI().toURL());
                featureUrls.put(path + keySuffix, list);
                return true;
            } catch (final MalformedURLException e) {
                // no-op
            }
        }
        return false;
    }

    private static void findWithCucumberSearcher(ClassLoader loader, String path, Collection<URL> list) {
        final MultiLoader multiLoader = new MultiLoader(loader);
        final Iterator<Resource> resources;
        try {
            resources = multiLoader.resources(path, EXTENSION).iterator();
        } catch (final IllegalArgumentException iae) { // not a directory...
            return;
        }

        while (resources.hasNext()) {
            final Resource resource = resources.next();

            if (FileResource.class.isInstance(resource)) {
                final FileResource fr = FileResource.class.cast(resource);
                try {
                    final Field field = FileResource.class.getDeclaredField("file");
                    field.setAccessible(true);
                    list.add(File.class.cast(field.get(fr)).toURI().toURL());
                } catch (final Exception e) {
                    // no-op
                }
            } else if (ZipResource.class.isInstance(resource)) {
                list.add(loader.getResource(resource.getPath()));
            } else {
                LOGGER.warning("Resource " + resource + " ignored (unknown type).");
            }
        }
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
            final CucumberOptions annotation = javaClass.getAnnotation(CucumberOptions.class);
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
