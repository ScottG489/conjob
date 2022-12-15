package conjob;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.RegistryAuth;
import conjob.config.JobConfig;
import conjob.core.job.*;
import conjob.core.job.config.ConfigUtil;
import conjob.core.secrets.SecretsContainerCreator;
import conjob.core.secrets.SecretsDockerAdapter;
import conjob.core.secrets.SecretsStore;
import conjob.core.secrets.TempSecretsFileUtil;
import conjob.healthcheck.VersionCheck;
import conjob.init.*;
import conjob.resource.GlobalErrorHandler;
import conjob.resource.GlobalExceptionMapper;
import conjob.resource.JobResource;
import conjob.resource.SecretsResource;
import conjob.resource.admin.task.ConfigMapper;
import conjob.resource.admin.task.ConfigTask;
import conjob.resource.admin.task.DockerVolumeRemoveTask;
import conjob.resource.convert.JobResponseConverter;
import conjob.resource.convert.ResponseCreator;
import conjob.resource.filter.EveryRequestFilter;
import conjob.resource.filter.EveryResponseFilter;
import conjob.resource.filter.MDCAdapter;
import conjob.service.job.*;
import conjob.service.secrets.SecretsService;
import conjob.service.secrets.UniqueContainerNameGenerator;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import org.glassfish.jersey.server.ServerProperties;

public class ConJobApplication extends Application<ConJobConfiguration> {
    @Getter
    private Environment environment;

    public static void main(String[] args) throws Exception {
        new ConJobApplication().run(args);
    }

    @Override
    public String getName() {
        return "ConJob";
    }

    @Override
    public void initialize(Bootstrap<ConJobConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(ConJobConfiguration configuration, Environment environment) throws DockerCertificateException, DockerException, InterruptedException {
        this.environment = environment;
        // TODO: We probably want to make this something higher than 0, though not too high
        environment.jersey().property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 0);
        MDCAdapter mdcAdapter = new MDCAdapter();
        environment.jersey().register(new EveryResponseFilter(mdcAdapter));
        environment.jersey().register(new EveryRequestFilter(mdcAdapter));

        DockerClient docker = createDockerClient(configuration);
        DockerAdapter dockerAdapter = new DockerAdapter(docker, configuration.getConjob().getDocker().getContainerRuntime());

        environment.jersey().register(
                createJobResource(
                        dockerAdapter,
                        configuration.getConjob().getJob().getLimit()));
        environment.jersey().register(createSecretsResource(docker));

        environment.admin().addTask(
                new ConfigTask(new ConfigStore(configuration.getConjob()), new ConfigMapper()));
        environment.admin().addTask(new DockerVolumeRemoveTask(dockerAdapter));

        environment.jersey().register(new GlobalExceptionMapper());
        environment.getApplicationContext().setErrorHandler(new GlobalErrorHandler());

        environment.healthChecks().register("version", new VersionCheck());

        new AdminBasicAuthConfigurator()
                .configureAdminBasicAuth(configuration.getConjob().getAdmin(), environment.admin());

        new BasicAuthConfigurator()
                .configureBasicAuth(configuration.getConjob().getAuth(), environment.jersey());
    }

    private DockerClient createDockerClient(ConJobConfiguration configuration) throws DockerCertificateException, DockerException, InterruptedException {
        DefaultDockerClient.Builder dockerBuilder = DefaultDockerClient.fromEnv();
        RegistryAuth.Builder authBuilder = RegistryAuth.builder();
        return new DockerClientCreator(
                dockerBuilder,
                new AuthedDockerClientCreator(dockerBuilder, authBuilder))
                .createDockerClient(
                        configuration.getConjob().getDocker().getUsername(),
                        configuration.getConjob().getDocker().getPassword());
    }

    private JobResource createJobResource(DockerAdapter dockerAdapter,
                                          JobConfig.LimitConfig limitConfig) {
        return new JobResource(
                new JobService(
                        createRunJobLimiter(limitConfig),
                        limitConfig,
                        new SecretsStore(dockerAdapter),
                        new JobRunCreationStrategyDeterminer(dockerAdapter),
                        new JobRunner(dockerAdapter),
                        new JobRunConfigCreator(),
                        new OutcomeDeterminer(),
                        new ConfigUtil(),
                        new ImageTagEnsurer()),
                new ResponseCreator(),
                new JobResponseConverter());
    }

    private RunJobLimiter createRunJobLimiter(JobConfig.LimitConfig limitConfig) {
        return new RunJobLimiter(
                new ConcurrentJobCountLimiter(limitConfig),
                new RunJobRateLimit(limitConfig));
    }

    private SecretsResource createSecretsResource(DockerClient docker) throws DockerException, InterruptedException {
        SecretsDockerAdapter secretsAdapter = new SecretsDockerAdapter(docker);
        return new SecretsResource(
                new SecretsService(
                        secretsAdapter,
                        new SecretsContainerCreator(secretsAdapter),
                        new TempSecretsFileUtil(),
                        new UniqueContainerNameGenerator(),
                        new ConfigUtil()));
    }
}
