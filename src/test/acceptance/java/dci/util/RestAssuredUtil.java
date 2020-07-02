package dci.util;

import io.restassured.RestAssured;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static dci.util.ConfigUtil.getFromConfig;

public class RestAssuredUtil {
    public static void setBaseUri() {
        RestAssured.baseURI = getFromConfig("baseUri");
    }

    public static void setAdminBaseUri() {
        RestAssured.baseURI = getFromConfig("adminBaseUri");
    }
}
