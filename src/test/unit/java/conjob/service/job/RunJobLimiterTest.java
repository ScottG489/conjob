package conjob.service.job;

import conjob.service.job.RunJobLimitMeter;
import conjob.service.job.RunJobLimiter;
import net.jqwik.api.*;
import net.jqwik.api.arbitraries.ArrayArbitrary;
import net.jqwik.api.constraints.NotEmpty;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class RunJobLimiterTest {

    @Property
    @Label("Given a list of meters that are all at the limit, " +
            "and it's not empty, " +
            "when we check if we are limiting, " +
            "then it should return true, " +
            "and none of the runs should be counted.")
    void allMetersAtLimit(@ForAll("allAtLimitMeters") @NotEmpty RunJobLimitMeter[] mockRunJobLimitMeters) {
        assertThat(new RunJobLimiter(mockRunJobLimitMeters).isLimitingOrIncrement(), is(true));
        Arrays.stream(mockRunJobLimitMeters).forEach(runJobLimitMeter ->
                verify(runJobLimitMeter, times(0)).countRun());
    }

    @Property
    @Label("Given a list of meters, " +
            "and none are at the limit, " +
            "when we check if we are limiting, " +
            "then it should return false, " +
            "and all of the runs should be counted.")
    void noMetersAtLimit(@ForAll("noneAtLimitMeters") RunJobLimitMeter[] mockRunJobLimitMeters) {
        assertThat(new RunJobLimiter(mockRunJobLimitMeters).isLimitingOrIncrement(), is(false));
        Arrays.stream(mockRunJobLimitMeters).forEach(runJobLimitMeter ->
                verify(runJobLimitMeter, times(1)).countRun());
    }

    @Property
    @Label("Given a list of meters, " +
            "and 1 or more is at the limit, " +
            "when we check if we are limiting, " +
            "then it should return true, " +
            "and at most 1 run should be counted.")
    void oneOrMoreMetersAtLimit(@ForAll("oneOrMoreAtLimitMeters") @NotEmpty RunJobLimitMeter[] mockRunJobLimitMeters) {
        assertThat(new RunJobLimiter(mockRunJobLimitMeters).isLimitingOrIncrement(), is(true));
        Arrays.stream(mockRunJobLimitMeters).forEach(runJobLimitMeter ->
                verify(runJobLimitMeter, atMost(1)).countRun());
    }

    @Property
    @Label("Given a list of meters, " +
            "when the job run is marked as complete, " +
            "then it should run job completion logic for all meters.")
    void markAllJobRunsComplete(@ForAll("limitMeters") @NotEmpty RunJobLimitMeter[] mockRunJobLimitMeters) {
        new RunJobLimiter(mockRunJobLimitMeters).markJobRunComplete();

        Arrays.stream(mockRunJobLimitMeters).forEach(runJobLimitMeter ->
                verify(runJobLimitMeter, times(1)).onJobComplete());
    }

    @Provide
    ArrayArbitrary<RunJobLimitMeter, RunJobLimitMeter[]> allAtLimitMeters() {
        return Arbitraries.ofSuppliers(() -> {
            RunJobLimitMeter mockRunJobLimitMeter = mock(RunJobLimitMeter.class);
            when(mockRunJobLimitMeter.isAtLimit()).thenReturn(true);
            return mockRunJobLimitMeter;
        })
                .array(RunJobLimitMeter[].class);
    }

    @Provide
    ArrayArbitrary<RunJobLimitMeter, RunJobLimitMeter[]> noneAtLimitMeters() {
        return Arbitraries.ofSuppliers(() -> {
            RunJobLimitMeter mockRunJobLimitMeter = mock(RunJobLimitMeter.class);
            when(mockRunJobLimitMeter.isAtLimit()).thenReturn(false);
            return mockRunJobLimitMeter;
        })
                .array(RunJobLimitMeter[].class);
    }

    @Provide
    Arbitrary<RunJobLimitMeter[]> oneOrMoreAtLimitMeters() {
        Arbitrary<Boolean> bool = Arbitraries.defaultFor(Boolean.class);
        return bool.map(b -> {
            RunJobLimitMeter mockRunJobLimitMeter = mock(RunJobLimitMeter.class);
            when(mockRunJobLimitMeter.isAtLimit()).thenReturn(b);
            return mockRunJobLimitMeter;
        })
                .collect(mocks -> mocks.stream().anyMatch(RunJobLimitMeter::isAtLimit))
                .map(meters -> meters.toArray(new RunJobLimitMeter[0]));
    }

    @Provide
    ArrayArbitrary<RunJobLimitMeter, RunJobLimitMeter[]> limitMeters() {
        return Arbitraries.ofSuppliers(() -> mock(RunJobLimitMeter.class)).array(RunJobLimitMeter[].class);
    }
}