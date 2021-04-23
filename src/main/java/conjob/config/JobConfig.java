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
        private Long maxGlobalRequestsPerSecond;
        private Long maxConcurrentRuns;
        private Long maxTimeoutSeconds;
        private Long maxKillTimeoutSeconds;
    }
}
