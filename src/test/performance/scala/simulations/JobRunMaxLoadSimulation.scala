import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import util.ConfigUtil

import scala.concurrent.duration.DurationInt

class JobRunMaxLoadSimulation extends Simulation {

  private val baseUrl: String = ConfigUtil.getFromConfig("baseUri")

  private val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
  private val request = http("Job run request")
    .get("/job/run")
    .queryParam("image", "${image}")
  private val maxLoadScenario: ScenarioBuilder = scenario("Job Run Simulation")
    .exec(_.set("image", "library/hello-world:latest"))
    .exec(request)

  private val warmUpRequest = request.silent
  private val warmUpScenario: ScenarioBuilder = scenario("Warm up Scenario")
    .exec(_.set("image", "library/hello-world:latest"))
    .exec(warmUpRequest)

  setUp(
    warmUpScenario.inject(
      constantUsersPerSec(1) during (1.seconds),
    ).andThen(
      maxLoadScenario.inject(
        atOnceUsers(5))
    ))
    .protocols(httpProtocol)
    .assertions(
      global.responseTime.mean.lt(5000),
      global.failedRequests.count.is(0)
    )
}
