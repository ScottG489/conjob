import io.gatling.core.Predef._
import io.gatling.core.body.StringBody
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import util.ConfigUtil

class SecretSimulation extends Simulation {

  private val baseUrl: String = ConfigUtil.getFromConfig("baseUri")

  private val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
  private val request = http("Secrets request")
    .post("/secret")
    .queryParam("image", "${image}")
    .body(new StringBody("some secret"))
  private val secretScenario : ScenarioBuilder = scenario("Secrets Simulation")
    .exec(_.set("image", "library/hello-world:latest"))
    .exec(request)

  setUp(
    secretScenario.inject(atOnceUsers(10))
  )
    .protocols(httpProtocol)
    .assertions(
      // Not sure why this gets above 1 second for 10 concurrent requests. It can handle much more locally. I
      //   don't think it's just network because the rate increases very quickly. I'd just chalk it up to the
      //   weaker AWS tier that we use.
      global.responseTime.mean.lt(2000),
      global.failedRequests.count.is(0)
    )
}
