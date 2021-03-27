package conjob;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.auth.FixedRegistryAuthSupplier;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.RegistryAuth;
import conjob.config.AdminConfig;
import conjob.config.AuthConfig;
import conjob.config.JobConfig;
import conjob.core.job.DockerAdapter;
import conjob.service.RunJobRateLimiter;
import conjob.healthcheck.VersionCheck;
import conjob.resource.GlobalErrorHandler;
import conjob.resource.GlobalExceptionMapper;
import conjob.resource.JobResource;
import conjob.resource.SecretResource;
import conjob.resource.admin.task.ConfigTask;
import conjob.resource.auth.AdminConstraintSecurityHandler;
import conjob.resource.auth.BasicAuthenticator;
import conjob.resource.filter.EveryResponseFilter;
import conjob.service.JobService;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.AdminEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.security.AbstractLoginService.UserPrincipal;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.util.Objects;

public class ConJobApplication extends Application<ConJobConfiguration> {
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
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(ConJobConfiguration configuration, Environment environment) throws DockerCertificateException, DockerException, InterruptedException {
        // TODO: We probably want to make this something higher than 0, though not too high
        environment.jersey().property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 0);
        environment.jersey().register(new EveryResponseFilter());

        DockerClient docker = createDockerClient(
                configuration.getConjob().getDocker().getUsername(),
                configuration.getConjob().getDocker().getPassword());

        environment.jersey().register(createJobResource(configuration.getConjob().getJob().getLimit(), docker));
        environment.jersey().register(new SecretResource(docker));

        environment.admin().addTask(new ConfigTask(configuration));

        environment.jersey().register(new GlobalExceptionMapper());
        environment.getApplicationContext().setErrorHandler(new GlobalErrorHandler());

        environment.healthChecks().register("version", new VersionCheck());

        configureAdminEnv(configuration.getConjob().getAdmin(), environment.admin());

        configureBasicAuth(configuration.getConjob().getAuth(), environment);
    }

    private DockerClient createDockerClient(String username, String password) throws DockerCertificateException, DockerException, InterruptedException {
        DefaultDockerClient docker;
        DefaultDockerClient.Builder dockerBuilder = DefaultDockerClient.fromEnv();
        if (Objects.nonNull(username) && Objects.nonNull(password)) {
            RegistryAuth registryAuth = RegistryAuth.builder()
                    .username(username)
                    .password(password)
                    .build();
            docker = dockerBuilder.registryAuthSupplier(
                    new FixedRegistryAuthSupplier(registryAuth, null))
                    .build();
            validateCredentials(docker, registryAuth);
        } else {
            docker = dockerBuilder.build();
        }

        return docker;
    }

    private void validateCredentials(DefaultDockerClient docker, RegistryAuth registryAuth) throws DockerException, InterruptedException {
        final int statusCode = docker.auth(registryAuth);
        if (statusCode != HttpStatus.SC_OK) {
            throw new RuntimeException("Incorrect docker credentials");
        }
    }

    private JobResource createJobResource(JobConfig.LimitConfig limitConfig, DockerClient docker) {
        return new JobResource(
                new JobService(
                        new DockerAdapter(docker),
                        new RunJobRateLimiter(limitConfig),
                        limitConfig));
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
                            config.getUsername(),
                            config.getPassword()));
        }
    }
}
