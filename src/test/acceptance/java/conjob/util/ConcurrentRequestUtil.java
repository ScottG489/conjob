package conjob.util;

import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static conjob.util.ConfigUtil.getFromConfig;
import static io.restassured.RestAssured.given;

public class ConcurrentRequestUtil {
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    public static List<Response> runConcurrentRequests(RequestSpecification requestSpec, int requestCount)
            throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        List<Future<Response>> futures = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            Future<Response> future = executor.submit(new SendRequest(requestSpec, Method.GET));
            futures.add(future);
        }
        executor.shutdownNow();

        List<Response> responses = new ArrayList<>();
        for (Future<Response> future : futures) {
            responses.add(future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        return responses;
    }

    static class SendRequest implements Callable<Response> {
        private final RequestSpecification requestSpec;
        private final Method httpMethod;

        public SendRequest(RequestSpecification requestSpec, Method httpMethod) {
            this.requestSpec = requestSpec;
            this.httpMethod = httpMethod;
        }

        @Override
        public Response call() {
            return given(requestSpec).request(httpMethod);
        }
    }
}
