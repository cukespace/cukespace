package cucumber.runtime.arquillian.shared;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class IOs {
    public static File dump(final String tempDir, final String s, final String feature) {
        FileWriter writer = null;
        try {
            final File file = new File(tempDir, s);
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                throw new IllegalArgumentException("Can't create '" + file.getAbsolutePath() + "'");
            }
            writer = new FileWriter(file);
            writer.write(feature);
            return file;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (final IOException e) {
                // no-p
            }
        }
    }

    public static byte[] slurp(final URL featureUrl) {
        try {
            return slurp(featureUrl.openStream());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] slurp(final InputStream is) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            final byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
        } catch (final IOException e) {
            // no-op
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // no-op
                }
            }
        }
        return baos.toByteArray();
    }

    private IOs() {
        // no-op
    }
}
