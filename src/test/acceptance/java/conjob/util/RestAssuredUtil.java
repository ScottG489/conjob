package conjob.util;

import io.restassured.RestAssured;

import static conjob.util.ConfigUtil.getFromConfig;

public class RestAssuredUtil {
    public static void configTest() {
        RestAssured.baseURI = getFromConfig("baseUri");
        RestAssured.useRelaxedHTTPSValidation();
    }

    public static void configAdminTest() {
        RestAssured.baseURI = getFromConfig("adminBaseUri");
        RestAssured.authentication = RestAssured.basic(
                getFromConfig("adminUsername"),
                getFromConfig("adminPassword"));
    }
}
