package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.WaitContainerCondition;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerAdapterWaitForContainerRemovalTest {
    private DockerAdapter dockerAdapter;
    private WaitContainerResultCallback mockCallback;

    @BeforeEach
    @BeforeTry
    void setUp() {
        DockerClient mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
        WaitContainerCmd mockCmd = mock(WaitContainerCmd.class);
        mockCallback = mock(WaitContainerResultCallback.class);
        when(mockClient.waitContainerCmd(anyString())).thenReturn(mockCmd);
        when(mockCmd.withCondition(WaitContainerCondition.REMOVED)).thenReturn(mockCmd);
        when(mockCmd.exec(any(WaitContainerResultCallback.class))).thenReturn(mockCallback);
    }

    @Property
    @Label("Given the container is removed within the timeout, " +
            "should complete without throwing.")
    void completesWithinTimeout(
            @ForAll String containerId,
            @ForAll @IntRange(min = 1, max = 60) int timeoutSeconds) throws InterruptedException {
        when(mockCallback.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertDoesNotThrow(() -> dockerAdapter.waitForContainerRemoval(containerId, timeoutSeconds));
    }

    @Property
    @Label("Given the wait times out, " +
            "should not throw.")
    void timesOut(
            @ForAll String containerId,
            @ForAll @IntRange(min = 1, max = 60) int timeoutSeconds) throws InterruptedException {
        when(mockCallback.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertDoesNotThrow(() -> dockerAdapter.waitForContainerRemoval(containerId, timeoutSeconds));
    }

    @Property
    @Label("Given the container is already removed (NotFoundException), " +
            "should not throw.")
    void notFoundException(
            @ForAll String containerId,
            @ForAll @IntRange(min = 1, max = 60) int timeoutSeconds) throws InterruptedException {
        when(mockCallback.awaitCompletion(anyLong(), any(TimeUnit.class)))
                .thenThrow(new NotFoundException(""));

        assertDoesNotThrow(() -> dockerAdapter.waitForContainerRemoval(containerId, timeoutSeconds));
    }

    @Property
    @Label("Given the wait is interrupted, " +
            "should not throw and should set the interrupt flag.")
    void interrupted(
            @ForAll String containerId,
            @ForAll @IntRange(min = 1, max = 60) int timeoutSeconds) throws InterruptedException {
        when(mockCallback.awaitCompletion(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException(""));

        assertDoesNotThrow(() -> dockerAdapter.waitForContainerRemoval(containerId, timeoutSeconds));
        assertTrue(Thread.interrupted(), "Interrupt flag should be set after handling InterruptedException");
    }

    @Property
    @Label("Given an unexpected exception during the wait, " +
            "should not throw.")
    void unexpectedException(
            @ForAll String containerId,
            @ForAll @IntRange(min = 1, max = 60) int timeoutSeconds) throws InterruptedException {
        when(mockCallback.awaitCompletion(anyLong(), any(TimeUnit.class)))
                .thenThrow(new RuntimeException(""));

        assertDoesNotThrow(() -> dockerAdapter.waitForContainerRemoval(containerId, timeoutSeconds));
    }
}
