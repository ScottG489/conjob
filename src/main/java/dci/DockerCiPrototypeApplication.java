package dci;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import dci.core.job.JobService;
import dci.healthcheck.VersionCheck;
import dci.resource.BuildResource;
import dci.resource.SecretResource;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.AdminEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.ServerProperties;

import java.util.Objects;

public class DockerCiPrototypeApplication extends Application<DockerCiPrototypeConfiguration> {
    public static void main(String[] args) throws Exception {
        new DockerCiPrototypeApplication().run(args);
    }

    @Override
    public String getName() {
        return "docker-ci-prototype";
    }

    @Override
    public void initialize(Bootstrap<DockerCiPrototypeConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(DockerCiPrototypeConfiguration configuration, Environment environment) throws DockerCertificateException, DockerException, InterruptedException {
        // TODO: We probably want to make this something higher than 0, though not too high
        environment.jersey().property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 0);
        environment.jersey().register(new EveryResponseFilter());

        final DockerClient docker = DefaultDockerClient.fromEnv().build();
        environment.jersey().register(new BuildResource(new JobService(docker)));
        environment.jersey().register(new SecretResource(docker));

        environment.healthChecks().register("version", new VersionCheck());

        configureAdminEnv(configuration, environment.admin());
    }

    private void configureAdminEnv(DockerCiPrototypeConfiguration config, AdminEnvironment adminEnv) {
        if (Objects.nonNull(config.getAdminUsername()) && Objects.nonNull(config.getAdminPassword())) {
            adminEnv.setSecurityHandler(
                    new AdminConstraintSecurityHandler(
                            config.getAdminUsername(),
                            config.getAdminPassword()));
        }
    }
}
