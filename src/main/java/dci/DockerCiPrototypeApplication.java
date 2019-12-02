package dci;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.messages.HostConfig;
import dci.resource.BuildResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.ServerProperties;

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
    }

    @Override
    public void run(DockerCiPrototypeConfiguration configuration, Environment environment) throws DockerCertificateException {
        // TODO: We probably want to make this something higher than 0, though not too high
        environment.jersey().property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 0);

        final DockerClient docker = DefaultDockerClient.fromEnv().build();
        HostConfig hostConfig = HostConfig.builder().binds("/var/run/docker.sock:/var/run/docker.sock").build();
        environment.jersey().register(new BuildResource(docker, hostConfig));

//        final HealthyHealthCheck healthCheck =
//                new HealthyHealthCheck(configuration.getTemplate());
//        environment.healthChecks().register("template", healthCheck);
    }
}
