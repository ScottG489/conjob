import io.gatling.core.Predef.{constantUsersPerSec, _}
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import util.ConfigUtil

import scala.concurrent.duration.DurationInt

class SecretsSimulation extends Simulation {

  private val baseUrl: String = ConfigUtil.getFromConfig("baseUri")

  private val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
  private val request = http("Secrets request")
    .post("/secrets")
    .queryParam("image", "${image}")
    .body(StringBody("some secret"))
  private val secretsScenario: ScenarioBuilder = scenario("Secrets Simulation")
    .exec(_.set("image", "library/hello-world:latest"))
    .exec(request)

  private val warmUpRequest = request.silent
  private val warmUpScenario: ScenarioBuilder = scenario("Warm up Scenario")
    .exec(_.set("image", "library/hello-world:latest"))
    .exec(warmUpRequest)

  private val all: ScenarioBuilder = warmUpScenario.exec(secretsScenario)

  setUp(
    warmUpScenario.inject(
      constantUsersPerSec(1) during (1.seconds),
    ).andThen(
      secretsScenario.inject(
        constantUsersPerSec(1) during (10.seconds))
    ))
    .protocols(httpProtocol)
    .assertions(
      // Not sure why this gets above 1 second for 10 concurrent requests. It can handle much more locally. I
      //   don't think it's just network because the rate increases very quickly. I'd just chalk it up to the
      //   weaker AWS tier that we use.
      global.responseTime.mean.lt(2000),
      global.failedRequests.count.is(0)
    )
}
