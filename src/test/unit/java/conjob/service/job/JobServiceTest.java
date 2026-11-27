package conjob.service.job;

import conjob.config.JobConfig;
import conjob.core.job.*;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.JobRunException;
import conjob.core.job.exception.JobUpdateException;
import conjob.core.job.model.JobRun;
import conjob.core.job.model.JobRunConclusion;
import conjob.core.job.model.JobRunConfig;
import conjob.core.job.model.JobRunOutcome;
import conjob.core.secrets.SecretsStore;
import conjob.core.secrets.SecretsStoreException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.UseType;
import net.jqwik.api.lifecycle.BeforeTry;
import testsupport.ExpectedLogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class JobServiceTest {
    private RunJobLimiter mockRunJobLimiter;
    private SecretsStore mockSecretsStore;
    private JobRunCreationStrategyDeterminer mockCreationStrategyDeterminer;
    private JobRunner mockJobRunner;
    private JobRunConfigCreator mockJobRunConfigCreator;
    private OutcomeDeterminer mockOutcomeDeterminer;
    private ConfigUtil mockConfigUtil;
    private JobConfig.LimitConfig limitConfig;
    private JobService jobService;
    private ImageTagEnsurer mockImageTagEnsurer;
    private JobCleaner mockJobCleaner;
    private ReentrantReadWriteLock imageLock;

    @BeforeTry
    void beforeEach() {
        mockRunJobLimiter = mock(RunJobLimiter.class);
        limitConfig = new JobConfig.LimitConfig();
        mockSecretsStore = mock(SecretsStore.class);
        mockCreationStrategyDeterminer = mock(JobRunCreationStrategyDeterminer.class);
        mockJobRunner = mock(JobRunner.class);
        mockJobRunConfigCreator = mock(JobRunConfigCreator.class);
        mockOutcomeDeterminer = mock(OutcomeDeterminer.class);
        mockConfigUtil = mock(ConfigUtil.class);
        mockImageTagEnsurer = mock(ImageTagEnsurer.class);
        mockJobCleaner = mock(JobCleaner.class);
        imageLock = new ReentrantReadWriteLock();
        jobService = new JobService(
                mockRunJobLimiter,
                limitConfig,
                mockSecretsStore,
                mockCreationStrategyDeterminer,
                mockJobRunner,
                mockJobRunConfigCreator,
                mockOutcomeDeterminer,
                mockConfigUtil,
                mockImageTagEnsurer,
                mockJobCleaner,
                imageLock.readLock()
        );
    }

    @Property
    @Label("Given a limiter that's not at the limit, " +
            "and the job concludes, " +
            "when the job is run, " +
            "should return a job run, " +
            "and it's fields should be from the run's conclusion and outcome.")
    void jobRunSuccessful(@ForAll String imageName,
                          @ForAll String input,
                          @ForAll("pullStrategyNames") String givenPullStrategyName,
                          @ForAll String givenDockerCacheVolumeName,
                          @ForAll String givenSecretsVolumeName,
                          @ForAll @UseType JobRunConfig givenJobRunConfig,
                          @ForAll String givenJobId,
                          @ForAll @UseType JobRunOutcome givenJobRunOutcome,
                          @ForAll JobRunConclusion givenJobRunConclusion,
                          @ForAll boolean givenRemove,
                          @ForAll boolean givenRemoveImage) throws SecretsStoreException, CreateJobRunException, JobUpdateException {
        boolean isLimiting = false;
        long maxTimeoutSeconds = limitConfig.getMaxTimeoutSeconds();
        int maxKillTimeoutSeconds = Math.toIntExact(limitConfig.getMaxKillTimeoutSeconds());
        PullStrategy pullStrategy = PullStrategy.valueOf(givenPullStrategyName.toUpperCase());
        JobRun expectedJobRun =
                new JobRun(givenJobRunConclusion, givenJobRunOutcome.getOutput(), givenJobRunOutcome.getExitStatusCode());
        JobRunCreationStrategy mockJobRunCreationStrategy = mock(JobRunCreationStrategy.class);
        mockCommonCallChain(imageName, input, givenDockerCacheVolumeName, givenSecretsVolumeName, givenJobRunConfig, isLimiting, pullStrategy, mockJobRunCreationStrategy);
        when(mockJobRunCreationStrategy.createJobRun(givenJobRunConfig)).thenReturn(givenJobId);
        when(mockJobRunner.runContainer(givenJobId, maxTimeoutSeconds, maxKillTimeoutSeconds, givenRemove || givenRemoveImage, givenRemoveImage))
                .thenReturn(givenJobRunOutcome);
        when(mockOutcomeDeterminer.determineOutcome(givenJobRunOutcome)).thenReturn(givenJobRunConclusion);

        JobRun jobRun = jobService.runJob(imageName, input, givenPullStrategyName, true, givenRemove, givenRemoveImage);

        assertThat(jobRun, is(expectedJobRun));
        verify(mockRunJobLimiter, times(1)).markJobRunComplete();
    }

    @Property
    @Label("Given removeImage is true, " +
            "and remove is false, " +
            "when the job is run, " +
            "should remove the container too.")
    void removeImageImpliesRemove(@ForAll String imageName,
                                  @ForAll String input,
                                  @ForAll("pullStrategyNames") String givenPullStrategyName,
                                  @ForAll String givenDockerCacheVolumeName,
                                  @ForAll String givenSecretsVolumeName,
                                  @ForAll @UseType JobRunConfig givenJobRunConfig,
                                  @ForAll String givenJobId,
                                  @ForAll @UseType JobRunOutcome givenJobRunOutcome) throws SecretsStoreException, CreateJobRunException, JobUpdateException {
        long maxTimeoutSeconds = limitConfig.getMaxTimeoutSeconds();
        int maxKillTimeoutSeconds = Math.toIntExact(limitConfig.getMaxKillTimeoutSeconds());
        PullStrategy pullStrategy = PullStrategy.valueOf(givenPullStrategyName.toUpperCase());
        JobRunCreationStrategy mockJobRunCreationStrategy = mock(JobRunCreationStrategy.class);
        mockCommonCallChain(imageName, input, givenDockerCacheVolumeName, givenSecretsVolumeName, givenJobRunConfig, false, pullStrategy, mockJobRunCreationStrategy);
        when(mockJobRunCreationStrategy.createJobRun(givenJobRunConfig)).thenReturn(givenJobId);
        when(mockJobRunner.runContainer(anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean()))
                .thenReturn(givenJobRunOutcome);

        jobService.runJob(imageName, input, givenPullStrategyName, true, false, true);

        verify(mockJobRunner).runContainer(givenJobId, maxTimeoutSeconds, maxKillTimeoutSeconds, true, true);
    }

    @Property
    @ExpectedLogs(JobService.class)
    @Label("Given a limiter that's not at the limit, " +
            "and creating a job run throws a create or update exception, " +
            "when the job is run, " +
            "and we try to create the job, " +
            "should return a not found job, " +
            "and mark the job run as completed in the limiter.")
    void jobNotFound(@ForAll String imageName,
                     @ForAll String input,
                     @ForAll("pullStrategyNames") String givenPullStrategyName,
                     @ForAll String givenDockerCacheVolumeName,
                     @ForAll String givenSecretsVolumeName,
                     @ForAll @UseType JobRunConfig givenJobRunConfig,
                     @ForAll("createOrUpdateJobRunException") JobRunException givenJobRunException,
                     @ForAll boolean givenRemove,
                     @ForAll boolean givenRemoveImage) throws SecretsStoreException, CreateJobRunException, JobUpdateException {
        boolean isLimiting = false;
        PullStrategy pullStrategy = PullStrategy.valueOf(givenPullStrategyName.toUpperCase());
        JobRunCreationStrategy mockJobRunCreationStrategy = mock(JobRunCreationStrategy.class);
        mockCommonCallChain(imageName, input, givenDockerCacheVolumeName, givenSecretsVolumeName, givenJobRunConfig, isLimiting, pullStrategy, mockJobRunCreationStrategy);
        when(mockJobRunCreationStrategy.createJobRun(givenJobRunConfig)).thenThrow(givenJobRunException);

        JobRun jobRun = jobService.runJob(imageName, input, givenPullStrategyName, true, givenRemove, givenRemoveImage);

        assertThat(jobRun, is(new JobRun(JobRunConclusion.NOT_FOUND, "", -1)));
        verify(mockRunJobLimiter, times(1)).markJobRunComplete();
        assertThat(imageLock.getReadLockCount(), is(0));
        // The image may have been pulled before the create failed.
        verify(mockJobCleaner, times(givenRemoveImage ? 1 : 0)).removeImageLater(imageName);
    }

    @Property
    @Label("Given remove and removeImage, " +
            "when the job is run, " +
            "should create the container with them, " +
            "with removeImage implying remove.")
    void containerConfigHasRemoveFlags(@ForAll String imageName,
                                       @ForAll String input,
                                       @ForAll("pullStrategyNames") String givenPullStrategyName,
                                       @ForAll String givenDockerCacheVolumeName,
                                       @ForAll String givenSecretsVolumeName,
                                       @ForAll @UseType JobRunConfig givenJobRunConfig,
                                       @ForAll String givenJobId,
                                       @ForAll @UseType JobRunOutcome givenJobRunOutcome,
                                       @ForAll boolean givenRemove,
                                       @ForAll boolean givenRemoveImage) throws SecretsStoreException, CreateJobRunException, JobUpdateException {
        PullStrategy pullStrategy = PullStrategy.valueOf(givenPullStrategyName.toUpperCase());
        JobRunCreationStrategy mockJobRunCreationStrategy = mock(JobRunCreationStrategy.class);
        mockCommonCallChain(imageName, input, givenDockerCacheVolumeName, givenSecretsVolumeName, givenJobRunConfig, false, pullStrategy, mockJobRunCreationStrategy);
        when(mockJobRunCreationStrategy.createJobRun(givenJobRunConfig)).thenReturn(givenJobId);
        when(mockJobRunner.runContainer(anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean()))
                .thenReturn(givenJobRunOutcome);

        jobService.runJob(imageName, input, givenPullStrategyName, true, givenRemove, givenRemoveImage);

        verify(mockJobRunConfigCreator).getContainerConfig(
                imageName, input, givenDockerCacheVolumeName, null, true, givenRemove || givenRemoveImage, givenRemoveImage);
        verify(mockJobCleaner, never()).removeImageLater(any());
    }

    @Property
    @Label("Given a limiter that's not at the limit, " +
            "when the job is run, " +
            "should hold the job start lock while creating the job run, " +
            "and release it before running the container.")
    void holdJobStartLockWhileCreating(@ForAll String imageName,
                                       @ForAll String input,
                                       @ForAll("pullStrategyNames") String givenPullStrategyName,
                                       @ForAll String givenDockerCacheVolumeName,
                                       @ForAll String givenSecretsVolumeName,
                                       @ForAll @UseType JobRunConfig givenJobRunConfig,
                                       @ForAll String givenJobId,
                                       @ForAll @UseType JobRunOutcome givenJobRunOutcome,
                                       @ForAll boolean givenRemove,
                                       @ForAll boolean givenRemoveImage) throws SecretsStoreException, CreateJobRunException, JobUpdateException {
        PullStrategy pullStrategy = PullStrategy.valueOf(givenPullStrategyName.toUpperCase());
        JobRunCreationStrategy mockJobRunCreationStrategy = mock(JobRunCreationStrategy.class);
        mockCommonCallChain(imageName, input, givenDockerCacheVolumeName, givenSecretsVolumeName, givenJobRunConfig, false, pullStrategy, mockJobRunCreationStrategy);
        List<Integer> readLocksWhileCreating = new ArrayList<>();
        List<Integer> readLocksWhileRunning = new ArrayList<>();
        when(mockJobRunCreationStrategy.createJobRun(givenJobRunConfig)).thenAnswer(invocation -> {
            readLocksWhileCreating.add(imageLock.getReadHoldCount());
            return givenJobId;
        });
        when(mockJobRunner.runContainer(anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean())).thenAnswer(invocation -> {
            readLocksWhileRunning.add(imageLock.getReadHoldCount());
            return givenJobRunOutcome;
        });

        jobService.runJob(imageName, input, givenPullStrategyName, true, givenRemove, givenRemoveImage);

        assertThat(readLocksWhileCreating, is(List.of(1)));
        assertThat(readLocksWhileRunning, is(List.of(0)));
    }

    @Property
    @Label("Given a limiter at the limit, " +
            "when the job is run, " +
            "should reject the job.")
    void rejectedJob(
            @ForAll String imageName,
            @ForAll String input,
            @ForAll("pullStrategyNames") String pullStrategyNames,
            @ForAll boolean givenRemove,
            @ForAll boolean givenRemoveImage) throws SecretsStoreException {
        when(mockRunJobLimiter.isLimitingOrIncrement()).thenReturn(true);

        JobRun jobRun = jobService.runJob(imageName, input, pullStrategyNames, true, givenRemove, givenRemoveImage);

        assertThat(jobRun, is(new JobRun(JobRunConclusion.REJECTED, "", -1)));
    }

    private void mockCommonCallChain(@ForAll String imageName,
                                     @ForAll String input,
                                     @ForAll String givenDockerCacheVolumeName,
                                     @ForAll String givenSecretsVolumeName,
                                     @UseType @ForAll JobRunConfig givenJobRunConfig,
                                     boolean isLimiting,
                                     PullStrategy pullStrategy,
                                     JobRunCreationStrategy mockJobRunCreationStrategy) throws SecretsStoreException {
        when(mockRunJobLimiter.isLimitingOrIncrement()).thenReturn(isLimiting);
        when(mockImageTagEnsurer.hasTagOrLatest(imageName)).thenReturn(imageName);
        when(mockConfigUtil.translateToSecretsVolumeName(imageName)).thenReturn(givenSecretsVolumeName);
        when(mockConfigUtil.translateToDockerCacheVolumeName(imageName)).thenReturn(givenDockerCacheVolumeName);
        when(mockSecretsStore.findSecrets(givenSecretsVolumeName)).thenReturn(Optional.empty());
        when(mockCreationStrategyDeterminer.determineStrategy(pullStrategy))
                .thenReturn(mockJobRunCreationStrategy);
        when(mockJobRunConfigCreator.getContainerConfig(
                eq(imageName), eq(input), eq(givenDockerCacheVolumeName), isNull(), eq(true), anyBoolean(), anyBoolean()))
                .thenReturn(givenJobRunConfig);
    }

    @Provide
    Arbitrary<JobRunException> createOrUpdateJobRunException() {
        return Arbitraries.of(
                new CreateJobRunException(new Exception()),
                new JobUpdateException(new Exception()));
    }

    @Provide
    Arbitrary<String> pullStrategyNames() {
        return Arbitraries.of(
                Arrays.stream(PullStrategy.values())
                        .map(Enum::name)
                        .map(String::chars)
                        .map(this::randomizeCase)
                        .toArray(String[]::new));
    }

    private String randomizeCase(IntStream is) {
        Random random = new Random();
        return is.mapToObj(i -> (char) i)
                .map(ch ->
                        random.nextBoolean() ? Character.toLowerCase(ch) : ch)
                .collect(Collector.of(
                        StringBuilder::new,
                        StringBuilder::append,
                        StringBuilder::append,
                        StringBuilder::toString));
    }
}
