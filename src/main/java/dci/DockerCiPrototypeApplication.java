package dci;

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
    public void run(DockerCiPrototypeConfiguration configuration, Environment environment) {
        // TODO: We probably want to make this something higher than 0, though not too high
        environment.jersey().property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 0);
        environment.jersey().register(new BuildResource());

//        final HealthyHealthCheck healthCheck =
//                new HealthyHealthCheck(configuration.getTemplate());
//        environment.healthChecks().register("template", healthCheck);
    }
}
