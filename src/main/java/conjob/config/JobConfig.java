package conjob.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobConfig {
    private LimitConfig limit;

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
}
