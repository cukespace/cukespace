package cucumber.runtime.arquillian;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AutoClientFeatureLoaderTest.class,
		ClientFeatureLoaderTest.class, CukesInBellyWithByClassConfigTest.class,
		CukesInBellyWithClassPathScanningFeatureTest.class,
		ServerFeatureLoaderTest.class, StepScanningTest.class })
public class CukeSpaceTestSuite {

}
