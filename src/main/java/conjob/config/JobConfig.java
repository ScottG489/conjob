package conjob.config;

import lombok.Data;

@Data
public class JobConfig {
    private LimitConfig limit;

    @Data
    public static class LimitConfig {
        private Long maxGlobalRequestsPerSecond;
        private Long maxTimeoutMinutes;
        private Long maxConcurrentRuns;
        private Long maxKillTimeoutSeconds;
    }
}
