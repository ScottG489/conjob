package dci;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class DockerCiPrototypeApplicationTest {
    @Test
    public void getName() {
        DockerCiPrototypeApplication app = new DockerCiPrototypeApplication();
        assertThat(app.getName(), is("docker-ci-prototype"));
    }
}