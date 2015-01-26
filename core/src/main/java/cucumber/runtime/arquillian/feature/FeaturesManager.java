package cucumber.runtime.arquillian.feature;

import cucumber.api.CucumberOptions;
import cucumber.runtime.arquillian.api.Features;
import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static cucumber.runtime.arquillian.shared.IOs.dump;
import static cucumber.runtime.arquillian.shared.IOs.slurp;
import static java.util.Arrays.asList;

public final class FeaturesManager {
    private static final Logger LOGGER = Logger.getLogger(FeaturesManager.class.getName());

    public static final String EXTENSION = ".feature";

    private FeaturesManager() {
        // no-op
    }

    public static Map<String, Collection<URL>> createFeatureMap(final String tempDir, final String featureHome,
                                                                final Class<?> javaClass, final ClassLoader classLoader) {
        final Map<String, Collection<URL>> featuresMap = new HashMap<String, Collection<URL>>();

        final String home = addSlashToHome(featureHome);

        final Features additionalFeaturesAnnotations = javaClass.getAnnotation(Features.class);
        final Collection<ResourceLoader> customResourceLoaders = retrieveCustomResourceLoaders(additionalFeaturesAnnotations);

        for (final String rawFeatureURL : findFeatures(javaClass)) {
        	featuresMap.putAll(extractUrlMapFromRawFeature(rawFeatureURL, home, tempDir, customResourceLoaders, classLoader, javaClass));        	           
        }
        
        featuresMap.putAll(extractUrlMapFromResourceLoaders(customResourceLoaders, null, tempDir, javaClass));
        
        LOGGER.fine("Features: " + featuresMap);

        return featuresMap;
    }
    
    private static String addSlashToHome(final String featureHome) {
    	if (featureHome != null && !featureHome.endsWith("/")) {
            return featureHome + "/";
        } else {
            return featureHome;
        }        
    }
    
    private static Collection<ResourceLoader> retrieveCustomResourceLoaders(final Features features) {
    	final Collection<ResourceLoader> customLoaders = new LinkedList<ResourceLoader>();
        customLoaders.addAll(CucumberLifecycle.resourceLoaders());
        if (features != null) {
            final Class<? extends ResourceLoader>[] userLoaders = features.loaders();
            for (final Class<? extends ResourceLoader> resourceLoader : userLoaders) {
                try {
                    final ResourceLoader instance = resourceLoader.newInstance();
                    customLoaders.add(instance);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("can't create a " + resourceLoader.getName(), e);
                }
            }
        }
        return customLoaders;
    }
    
    private static Collection<String> findFeatures(final Class<?> javaClass) {
        final Collection<String> featureUrls = new ArrayList<String>();
        { // convention
            final String featurePath = getfeaturePath(javaClass);
            featureUrls.add(featurePath);
        }
        { // our API
            final Features additionalFeaturesAnnotation = javaClass.getAnnotation(Features.class);
            if (additionalFeaturesAnnotation != null) {
                Collections.addAll(featureUrls, additionalFeaturesAnnotation.value());
            }
        }        
        { // cucumber-junit API
            final CucumberOptions cucumberOptionsAnnotation = javaClass.getAnnotation(CucumberOptions.class);
            if (cucumberOptionsAnnotation != null && cucumberOptionsAnnotation.features() != null) {
                Collections.addAll(featureUrls, cucumberOptionsAnnotation.features());
            }
        }
        return featureUrls;
    }
    
    private static String getfeaturePath(final Class<?> javaClass) {
        return javaClass.getPackage().getName().replace('.', '/') + '/' + createClassNameSubPackage(javaClass.getSimpleName()) + EXTENSION;
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
    
    private static Map<String, Collection<URL>> extractUrlMapFromRawFeature(final String rawFeatureURL, final String featureHome, final String tempDir, 
    		final Collection<ResourceLoader> customResourceLoaders,final ClassLoader classLoader, final Class<?> javaClass) {

    	final Map<String, Collection<URL>> featureURLMap = new HashMap<String, Collection<URL>>();
        final String urlPath = extractFeatureURLPath(getSeparatorIndex(rawFeatureURL), rawFeatureURL);
        final boolean directResource = urlPath.endsWith(EXTENSION);
        
        if (directResource) {
        	try {
        		featureURLMap.putAll(extractUrlMapFromDirectResource(rawFeatureURL,featureHome,classLoader));            		
        		return featureURLMap;
        	} catch(IllegalStateException ise)
        	{
        		//do nothing
        	}
            
            final String resourcePath=urlPath.substring(0, urlPath.length() - EXTENSION.length());
            featureURLMap.putAll(extractUrlMapFromResourceLoaders(customResourceLoaders, resourcePath, tempDir, javaClass));                
        }
        
        try {
        	featureURLMap.putAll(extractUrlMapFromCucumberSearcher(classLoader, rawFeatureURL, featureHome));            	
        } catch (IllegalStateException ise) {
        	//do nothing
        }
        return featureURLMap;
    }
    
    private static Map<String, Collection<URL>> extractUrlMapFromDirectResource(final String rawFeatureURL, final String featureHome, final ClassLoader classLoader) throws IllegalStateException {
    	final Map<String, Collection<URL>> featureURLMap = new HashMap<String, Collection<URL>>();
    	
    	int lineSeparatorIndex = getSeparatorIndex(rawFeatureURL);
        final String urlPath = extractFeatureURLPath(lineSeparatorIndex, rawFeatureURL);
        final String urlSuffix = extractFeatureURLSuffix(lineSeparatorIndex, rawFeatureURL);
    	
        try {
    		featureURLMap.put(rawFeatureURL + urlSuffix, createUrlsListFromClassPath(classLoader, urlPath));
    		return featureURLMap;
    	} catch(NullPointerException npe) {
    		//do nothing
    	}
    	
    	try {
    		featureURLMap.put(urlPath + urlSuffix, createUrlsListFromFileSystem(urlPath));
    		return featureURLMap;    		
    	} catch(NullPointerException npe) {
    		//do nothing
    	}

        // from filesystem with featureHome
    	try {
    		featureURLMap.put(urlPath + urlSuffix, createUrlsListFromFileSystem(featureHome + urlPath));
    		return featureURLMap;
    	} catch(NullPointerException npe) {
    		//do nothing
    	}
    	throw new IllegalStateException();
    }
    
    private static List<URL> createUrlsListFromClassPath(final ClassLoader classLoader, final String urlPath) throws NullPointerException {
    	final List<URL> urlsList = new ArrayList<URL>();
    	urlsList.add(extractURLFromClassPath(classLoader, urlPath));
    	return urlsList;
    }
    
    private static URL extractURLFromClassPath(final ClassLoader classLoader, final String urlPath) throws NullPointerException {
    	final URL url = classLoader.getResource(urlPath);
    	if(url==null) {
    		throw new NullPointerException();
    	}
        return url;
    }
    
    private static List<URL> createUrlsListFromFileSystem(final String filePath) throws NullPointerException {
    	final List<URL> urlsList = new ArrayList<URL>();
    	urlsList.add(extractURLFromFileSystem(filePath));
    	return urlsList;
    }
    
    private static URL extractURLFromFileSystem(final String filePath) throws NullPointerException {
    	final File file = new File(filePath);
        if (file.exists() && !file.isDirectory()) {
            try {
                final URL url = file.toURI().toURL();
                return url;
            } catch (final MalformedURLException e) {
                //do nothing
            }
        }
        throw new NullPointerException();
    }

    private static Map<String, Collection<URL>> extractUrlMapFromResourceLoaders(final Collection<ResourceLoader> resourceLoaders, 
    		final String resourcePath, final String tempDir, final Class<?> javaClass) {
    	final Map<String, Collection<URL>> featureURLMap = new HashMap<String, Collection<URL>>();
    	for (final ResourceLoader resourceLoader : resourceLoaders) {
    		try {
    			for (final Resource resource : resourceLoader.resources(resourcePath, EXTENSION)) {
                    try {
                        final File featureFileDump = getFeatureFileDumpFromResource(resource, tempDir, javaClass);
                        featureURLMap.put(resource.getPath(), asList(featureFileDump.toURI().toURL()));
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
    	return featureURLMap;
    }
    
    private static File getFeatureFileDumpFromResource(final Resource resource, final String tempDir, final Class<?> javaClass) throws IOException {
    	final String feature = new String(slurp(resource.getInputStream()));
        final String featurePath = resource.getPath();
        final File featureFileDump = dump(tempDir, javaClass.getName() + '/' + featurePath, feature);
        featureFileDump.deleteOnExit();
        return featureFileDump;
    }
    
    private static Map<String, Collection<URL>> extractUrlMapFromCucumberSearcher(final ClassLoader classLoader, final String rawFeatureURL, final String featureHome) throws IllegalStateException {
    	final Map<String, Collection<URL>> featureURLMap = new HashMap<String, Collection<URL>>();    	
    	int lineSeparatorIndex = getSeparatorIndex(rawFeatureURL);
        final String urlPath = extractFeatureURLPath(lineSeparatorIndex, rawFeatureURL);
        final String urlSuffix = extractFeatureURLSuffix(lineSeparatorIndex, rawFeatureURL);    	
        featureURLMap.put(urlPath + urlSuffix, createListFromCucumberSearcher(classLoader, featureHome, urlPath));    	
        return featureURLMap;
    }
    
    private static List<URL> createListFromCucumberSearcher(final ClassLoader classLoader, final String featuresHome, final String urlPath) throws IllegalStateException {
    	final List<URL> urlsList = new ArrayList<URL>();        
        try {
        	urlsList.addAll(createUrlsListFromResource(classLoader, urlPath));
        } catch(Exception e)
        {
        	//do nothing
        }
        
        try {
        	urlsList.addAll(createUrlsListFromResource(classLoader, featuresHome+urlPath));
        } catch(Exception e)
        {
        	//do nothing
        }
        
        if(urlsList.isEmpty()) { 
        	throw new IllegalStateException();
        }        
        return urlsList;
    }
    
    private static List<URL> createUrlsListFromResource(final ClassLoader classLoader, final String resourcesPath) throws IllegalStateException {
    	final List<URL> urlsList=new ArrayList<URL>();
    	final MultiLoader multiLoader = new MultiLoader(classLoader);
        final Iterator<Resource> resources;
        try {
        	resources = multiLoader.resources(resourcesPath, EXTENSION).iterator();
        } catch (final IllegalArgumentException iae) { // not a directory...
            throw new IllegalStateException();
        }

        while (resources.hasNext()) {
            final Resource resource = resources.next();
            final URL extractedURL = extractUrlFromResource(resource, classLoader);
            
            if(extractedURL!=null) {
            	urlsList.add(extractedURL);
            } else {
            	LOGGER.warning("Resource " + resource + " ignored (unknown type).");
            }            
        }
        
        if(urlsList.isEmpty()) {
        	throw new IllegalStateException();
        }        
        return urlsList;
    }
    
    private static URL extractUrlFromResource(final Resource resource, final ClassLoader classLoader) {    	
    	if (FileResource.class.isInstance(resource)) {            
            return extractUrlFromFileResource(FileResource.class.cast(resource));
        } else if (ZipResource.class.isInstance(resource)) {
            return extractUrlFromZipResource(resource, classLoader);
        } else {
            return null;
        }
    }
    
    private static URL extractUrlFromFileResource(final FileResource fileResource) {    	
    	try {
            final Field field = FileResource.class.getDeclaredField("file");
            field.setAccessible(true);
            final URL url = File.class.cast(field.get(fileResource)).toURI().toURL();
            return url;
        } catch (final Exception e) {
        	return null;
        }
    }
    
    private static URL extractUrlFromZipResource(final Resource resource, final ClassLoader classLoader) {
    	final URL url = classLoader.getResource(resource.getPath());
        return url;
    }
    
    private static int getSeparatorIndex(final String rawFeatureURL) {
    	return rawFeatureURL.lastIndexOf(':');
    }
        
    private static String extractFeatureURLPath(final int lineSeparatorIndex, final String rawFeatureURL) {
    	final String path;     	
    	if (lineSeparatorIndex > 0 && lineSeparatorIndex + 1 != MultiLoader.CLASSPATH_SCHEME.length()) {
             path = rawFeatureURL.substring(0, lineSeparatorIndex);             
        } else {             
             path = rawFeatureURL;
        }    	
    	return path;
    }
    
    private static String extractFeatureURLSuffix(final int lineSeparatorIndex, final String rawFeatureURL) {
    	final String suffix;    	
    	if (lineSeparatorIndex > 0 && lineSeparatorIndex + 1 != MultiLoader.CLASSPATH_SCHEME.length()) {
             suffix = rawFeatureURL.substring(lineSeparatorIndex);
        } else {
             suffix = "";
        }    	
    	return suffix;
    }
}
