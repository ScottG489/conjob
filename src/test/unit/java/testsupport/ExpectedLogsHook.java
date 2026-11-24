package testsupport;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import net.jqwik.api.lifecycle.*;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Applies {@link ExpectedLogs} to every jqwik property. It's registered in
 * META-INF/services/net.jqwik.api.lifecycle.LifecycleHook rather than by the annotation, so that it also checks
 * properties that don't have the annotation.
 */
public class ExpectedLogsHook implements AroundPropertyHook, AroundTryHook {
    private static final Logger APPLICATION_LOGGER = (Logger) LoggerFactory.getLogger("conjob");

    @Override
    public PropagationMode propagateTo() {
        return PropagationMode.ALL_DESCENDANTS;
    }

    @Override
    public PropertyExecutionResult aroundProperty(PropertyLifecycleContext context, PropertyExecutor property) {
        Class<?>[] expectedClasses = context.findAnnotation(ExpectedLogs.class)
                .map(ExpectedLogs::value)
                .orElse(new Class<?>[0]);
        LogCapture capture = Store.create(LogCapture.class, Lifespan.PROPERTY, () -> new LogCapture(expectedClasses))
                .get();

        capture.start();
        PropertyExecutionResult result;
        try {
            result = property.execute();
        } finally {
            capture.stop();
        }
        if (result.status() == PropertyExecutionResult.Status.FAILED) {
            capture.printLastFalsifiedTry();
        }
        return result;
    }

    @Override
    public TryExecutionResult aroundTry(TryLifecycleContext context, TryExecutor aTry, List<Object> parameters) {
        LogCapture capture = Store.<LogCapture>get(LogCapture.class).get();
        capture.clear();
        TryExecutionResult result = aTry.execute(parameters);
        if (result.isFalsified()) {
            capture.keepAsLastFalsifiedTry();
            return result;
        }
        return capture.firstUnexpectedWarning()
                .map(ExpectedLogsHook::unexpectedWarningFailure)
                .orElse(result);
    }

    private static TryExecutionResult unexpectedWarningFailure(ILoggingEvent event) {
        String loggerName = event.getLoggerName();
        String className = loggerName.substring(loggerName.lastIndexOf('.') + 1).replace('$', '.');
        Throwable logged = event.getThrowableProxy() instanceof ThrowableProxy proxy ? proxy.getThrowable() : null;
        return TryExecutionResult.falsified(new AssertionError(String.format(
                "Unexpected %s logged by %s: %s%n" +
                        "If this property causes it on purpose, annotate it with @ExpectedLogs(%s.class)",
                event.getLevel(), loggerName, event.getFormattedMessage(), className), logged));
    }

    private static class LogCapture {
        private final List<Logger> expectedLoggers;
        private final List<Boolean> expectedLoggersWereAdditive = new ArrayList<>();
        private final ListAppender<ILoggingEvent> expected = new ListAppender<>();
        private final ListAppender<ILoggingEvent> unexpected = new ListAppender<>();
        private List<ILoggingEvent> lastFalsifiedTry = List.of();

        LogCapture(Class<?>[] expectedClasses) {
            expectedLoggers = Arrays.stream(expectedClasses)
                    .map(expectedClass -> (Logger) LoggerFactory.getLogger(expectedClass))
                    .toList();
        }

        void start() {
            expected.setContext(APPLICATION_LOGGER.getLoggerContext());
            unexpected.setContext(APPLICATION_LOGGER.getLoggerContext());
            expected.start();
            unexpected.start();
            for (Logger logger : expectedLoggers) {
                expectedLoggersWereAdditive.add(logger.isAdditive());
                logger.addAppender(expected);
                // Keeps their logs out of the console, and away from the "unexpected" appender on their ancestor
                logger.setAdditive(false);
            }
            APPLICATION_LOGGER.addAppender(unexpected);
        }

        void stop() {
            APPLICATION_LOGGER.detachAppender(unexpected);
            for (int i = 0; i < expectedLoggers.size(); i++) {
                expectedLoggers.get(i).setAdditive(expectedLoggersWereAdditive.get(i));
                expectedLoggers.get(i).detachAppender(expected);
            }
        }

        void clear() {
            expected.list.clear();
            unexpected.list.clear();
        }

        void keepAsLastFalsifiedTry() {
            lastFalsifiedTry = new ArrayList<>(expected.list);
        }

        Optional<ILoggingEvent> firstUnexpectedWarning() {
            return unexpected.list.stream()
                    .filter(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                    .findFirst();
        }

        // Sends the events to the console as if they hadn't been captured
        void printLastFalsifiedTry() {
            for (ILoggingEvent event : lastFalsifiedTry) {
                ((Logger) LoggerFactory.getLogger(event.getLoggerName())).callAppenders(event);
            }
        }
    }
}
