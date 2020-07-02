package dci.resource;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;

import static dci.util.RestAssuredUtil.setBaseUri;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class BuildTest {
    @Before
    public void setup() {
        setBaseUri();
    }

    @Test
    public void getName() {
        String expectContains = "Hello from Docker!";

        String responseBody =
                RestAssured.get("/build?image=library/hello-world:latest").body().asString();

        assertThat(responseBody, containsString(expectContains));
    }
}