package org.wso2.carbon.inbound.endpoint.protocol;

import org.apache.axis2.util.GracefulShutdownTimer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);

    /**
     * Waits for the completion of all in-flight RabbitMQ tasks during a graceful shutdown.
     * The method blocks until either all in-flight messages are processed or the graceful
     * shutdown timer expires, whichever comes first. This ensures that message processing
     * is completed as much as possible before shutting down the consumer. Also, to ensure
     * the waiting loop doesn't run indefinitely due to unexpected conditions, a fallback
     * check is also introduced.
     *
     * @param gracefulShutdownTimer the {@link GracefulShutdownTimer} instance controlling the shutdown timeout
     */
    public static void waitForGracefulTaskCompletion(GracefulShutdownTimer gracefulShutdownTimer,
                                                     AtomicInteger inFlightMessages, String inboundEndpointName) {

        long startTimeMillis = System.currentTimeMillis();
        long timeoutMillis = gracefulShutdownTimer.getShutdownTimeoutMillis();

        // If the server is shutting down, we wait until either all in-flight messages are done
        // or the graceful shutdown timer expires (whichever comes first)
        while (inFlightMessages.get() > 0 && !gracefulShutdownTimer.isExpired()) {
            try {
                Thread.sleep(100); // wait until all in-flight messages are done
            } catch (InterruptedException e) {}

            // Safety check: Ensure the loop doesn't run indefinitely due to unexpected conditions.
            // This fallback check ensures that if the timer somehow fails to expire as expected,
            // the loop can still exit gracefully once the configured timeout period has elapsed.
            if ((System.currentTimeMillis() - startTimeMillis) >= timeoutMillis) {
                log.warn("Graceful shutdown timer elapsed. Exiting waiting loop to prevent "
                        + "indefinite blocking.");
                break;
            }
        }
    }

}
