package conjob;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.RegistryAuth;
import conjob.config.AdminConfig;
import conjob.config.AuthConfig;
import conjob.config.JobConfig;
import conjob.core.job.DockerAdapter;
import conjob.core.job.JobRunConfigCreator;
import conjob.core.job.JobRunner;
import conjob.core.job.OutcomeDeterminer;
import conjob.core.job.config.ConfigUtil;
import conjob.core.secret.SecretStore;
import conjob.healthcheck.VersionCheck;
import conjob.init.AuthedDockerClientCreator;
import conjob.init.DockerClientCreator;
import conjob.resource.GlobalErrorHandler;
import conjob.resource.GlobalExceptionMapper;
import conjob.resource.JobResource;
import conjob.resource.SecretResource;
import conjob.resource.admin.task.ConfigTask;
import conjob.resource.auth.AdminConstraintSecurityHandler;
import conjob.resource.auth.BasicAuthenticator;
import conjob.resource.convert.JobResponseConverter;
import conjob.resource.convert.ResponseCreator;
import conjob.resource.filter.EveryResponseFilter;
import conjob.service.*;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.AdminEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import org.eclipse.jetty.security.AbstractLoginService.UserPrincipal;
import org.eclipse.jetty.util.security.Password;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.util.Objects;

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
        environment.jersey().register(new EveryResponseFilter());

        DockerClient docker = createDockerClient(configuration);

        environment.jersey().register(
                createJobResource(configuration.getConjob().getJob().getLimit(), docker));
        environment.jersey().register(new SecretResource(docker));

        environment.admin().addTask(new ConfigTask(configuration));

        environment.jersey().register(new GlobalExceptionMapper());
        environment.getApplicationContext().setErrorHandler(new GlobalErrorHandler());

        environment.healthChecks().register("version", new VersionCheck());

        configureAdminEnv(configuration.getConjob().getAdmin(), environment.admin());

        configureBasicAuth(configuration.getConjob().getAuth(), environment);
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

    private JobResource createJobResource(JobConfig.LimitConfig limitConfig, DockerClient docker) {
        DockerAdapter dockerAdapter = new DockerAdapter(docker);
        return new JobResource(
                new JobService(
                        createRunJobLimiter(limitConfig),
                        limitConfig,
                        new SecretStore(dockerAdapter),
                        new JobRunCreationStrategyDeterminer(dockerAdapter),
                        new JobRunner(dockerAdapter),
                        new JobRunConfigCreator(),
                        new OutcomeDeterminer(),
                        new ConfigUtil()),
                new ResponseCreator(),
                new JobResponseConverter());
    }

    private RunJobLimiter createRunJobLimiter(JobConfig.LimitConfig limitConfig) {
        return new RunJobLimiter(
                new ConcurrentJobCountLimiter(limitConfig),
                new RunJobRateLimit(limitConfig));
    }

    private void configureBasicAuth(AuthConfig config, Environment environment) {
        if (Objects.nonNull(config.getUsername()) && Objects.nonNull(config.getPassword())) {
            environment.jersey().register(new AuthDynamicFeature(
                    new BasicCredentialAuthFilter.Builder<UserPrincipal>()
                            .setAuthenticator(new BasicAuthenticator(config.getUsername(), config.getPassword()))
                            .buildAuthFilter()));
            environment.jersey().register(RolesAllowedDynamicFeature.class);
        }
    }

    private void configureAdminEnv(AdminConfig config, AdminEnvironment adminEnv) {
        if (Objects.nonNull(config.getUsername()) && Objects.nonNull(config.getPassword())) {
            adminEnv.setSecurityHandler(
                    new AdminConstraintSecurityHandler(
                            new AdminConstraintSecurityHandler.AdminLoginService(
                                    new UserPrincipal(
                                            config.getUsername(),
                                            new Password(config.getPassword())))));
        }
    }
}
