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
      global.responseTime.mean.lt(1000),
      global.failedRequests.count.is(0)
    )
}
