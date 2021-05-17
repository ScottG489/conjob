package conjob.service;

import conjob.core.job.*;

import java.util.Map;

public class JobRunCreationStrategyDeterminer {
    private final Map<PullStrategy, JobRunCreationStrategy> pullStrategyJobCreationStrategy;

    public JobRunCreationStrategyDeterminer(DockerAdapter dockerAdapter) {
        pullStrategyJobCreationStrategy =
                Map.of(PullStrategy.ALWAYS, new AlwaysPullStrategy(dockerAdapter),
                        PullStrategy.NEVER, new NeverPullStrategy(dockerAdapter),
                        PullStrategy.ABSENT, new AbsentPullStrategy(dockerAdapter));
    }

    public JobRunCreationStrategy determineStrategy(PullStrategy pullStrategy) {
        return pullStrategyJobCreationStrategy.get(pullStrategy);
    }
}
