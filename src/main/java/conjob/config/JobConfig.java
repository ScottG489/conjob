package conjob.config;

import io.dropwizard.util.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobConfig {
    private LimitConfig limit;
    private CleanupConfig cleanup = new CleanupConfig();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LimitConfig {
        private Long maxGlobalRequestsPerSecond = Long.MAX_VALUE;
        private Long maxConcurrentRuns = Long.MAX_VALUE;
        private Long maxTimeoutSeconds = Long.MAX_VALUE;
        // TODO: This field should be an integer but complications with ConfigTask and it's tests
        // TODO:   are preventing this right now.
        private Long maxKillTimeoutSeconds = (long) Integer.MAX_VALUE;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CleanupConfig {
        // How often to sweep up containers and images that jobs left behind, and dangling images.
        // The first sweep is at startup. Zero turns sweeping off.
        private Duration sweepInterval = Duration.hours(1);
    }
}
