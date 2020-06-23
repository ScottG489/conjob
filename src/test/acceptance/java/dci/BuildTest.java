package dci;

import io.restassured.RestAssured;
import org.junit.Test;

import static dci.util.ConfigUtil.getFromConfig;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class BuildTest {
    static {
        RestAssured.baseURI = getFromConfig("baseUri");
    }

    @Test
    public void getName() {
        String expectContains = "Hello from Docker!";

        String responseBody =
                RestAssured.get("/build?image=library/hello-world:latest").body().asString();

        assertThat(responseBody, containsString(expectContains));
    }
}