package cucumber.runtime.arquillian.stream;

import java.io.PrintStream;

public class NotCloseablePrintStream extends PrintStream {
    public NotCloseablePrintStream(final PrintStream originalOut) {
        super(originalOut);
    }

    @Override
    public void close() {
        flush();
    }
}
