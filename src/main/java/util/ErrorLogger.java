package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ErrorLogger {

    private static final Logger LOGGER =
            LogManager.getLogger(ErrorLogger.class);

    private ErrorLogger() {
        // Utility class.
    }

    public static void log(Throwable throwable) {

        if (throwable == null) {
            return;
        }

        LOGGER.error(
                "Unhandled application exception",
                throwable
        );
    }
}