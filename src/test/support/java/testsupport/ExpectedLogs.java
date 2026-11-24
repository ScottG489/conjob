package testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a property that makes the given classes log warnings or errors on purpose, e.g. by having a mock throw an
 * exception that they handle. jqwik runs a property many times, and their stack traces would fill the build output,
 * so what they log is captured instead of printed. If the property fails, the logs from its last falsified try are
 * printed.
 * <p>
 * {@link ExpectedLogsHook} fails a property in which ConJob's code logs a warning or error that the property doesn't
 * expect, so a test of an error path can't be left without this annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExpectedLogs {
    /**
     * The classes that log on purpose in this property
     */
    Class<?>[] value();
}
