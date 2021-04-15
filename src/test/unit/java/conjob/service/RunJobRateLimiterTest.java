//package conjob.service;
//
//import conjob.config.JobConfig;
//import conjob.service.RunJobRateLimiter;
//import net.jqwik.api.Assume;
//import net.jqwik.api.ForAll;
//import net.jqwik.api.Label;
//import net.jqwik.api.constraints.LongRange;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.*;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//
//class RunJobRateLimiterTest {
//
//    //    @Property
//    @Label("Given more concurrent requests than the max allowed, " +
//            "when all requests are sent, " +
//            "then the requests up to the max are not at the limit, " +
//            "and the requests after are considered over the limit")
//    boolean isAtLimitConcurrentRequestsGreaterThanMax(
//            @ForAll @LongRange(min = 0, max = 100) long maxConcurrentRuns,
//            @ForAll @LongRange(min = 0, max = 100) long concurrentRequests) {
//        Assume.that(() -> maxConcurrentRuns < concurrentRequests);
//
//        long requestsOverLimit = concurrentRequests - maxConcurrentRuns;
//
//        long maxGlobalRequestsPerSecond = Integer.MAX_VALUE;
//        long maxTimeoutSeconds = Integer.MAX_VALUE;
//        long maxKillTimeoutSeconds = Integer.MAX_VALUE;
//
//        JobConfig.LimitConfig limitConfig = new JobConfig.LimitConfig(
//                maxGlobalRequestsPerSecond, maxConcurrentRuns, maxTimeoutSeconds, maxKillTimeoutSeconds);
//        RunJobRateLimiter runJobRateLimiter = new RunJobRateLimiter(limitConfig);
//
//        for (int i = 0; i < maxConcurrentRuns; i++) {
//            assertThat(runJobRateLimiter.isAtLimit(), is(false));
//        }
//
//        for (int i = 0; i < requestsOverLimit; i++) {
//            assertThat(runJobRateLimiter.isAtLimit(), is(true));
//        }
//
//        return true;
//    }
//
//    //    @Property
//    @Label("Given more concurrent requests than the max allowed, " +
//            "when all requests are sent, " +
//            "then the requests up to the max are not at the limit, " +
//            "and the requests after are considered over the limit")
//    boolean isAtLimitConcurrentRequestsGreaterThanMax(
//            @ForAll @LongRange(min = 0, max = 100) long concurrentRequests) {
//        long maxConcurrentRuns = Integer.MAX_VALUE;
//        long maxGlobalRequestsPerSecond = Integer.MAX_VALUE;
//        long maxTimeoutSeconds = Integer.MAX_VALUE;
//        long maxKillTimeoutSeconds = Integer.MAX_VALUE;
//
//        JobConfig.LimitConfig limitConfig = new JobConfig.LimitConfig(
//                maxGlobalRequestsPerSecond, maxConcurrentRuns, maxTimeoutSeconds, maxKillTimeoutSeconds);
//        RunJobRateLimiter runJobRateLimiter = new RunJobRateLimiter(limitConfig);
//
//        for (int i = 0; i < maxConcurrentRuns; i++) {
//            assertThat(runJobRateLimiter.isAtLimit(), is(false));
//        }
//
////        for (int i = 0; i < requestsOverLimit; i++) {
////            assertThat(runJobRateLimiter.isAtLimit(), is(true));
////        }
//
//        return true;
//    }
//
//    public static List<Boolean> runConcurrentRequests(RunJobRateLimiter rateLimiter, int requestCount)
//            throws InterruptedException, ExecutionException, TimeoutException {
//        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
//        List<Future<Boolean>> futures = new ArrayList<>();
//        for (int i = 0; i < requestCount; i++) {
//            Future<Boolean> future = executor.submit(() -> rateLimiter.isAtLimit());
//            futures.add(future);
//        }
//        executor.shutdownNow();
//
//        List<Boolean> responses = new ArrayList<>();
//        for (Future<Boolean> future : futures) {
//            responses.add(future.get(5L, TimeUnit.SECONDS));
//        }
//
//        return responses;
//    }
//}