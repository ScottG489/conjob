import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import util.ConfigUtil

class JobRunOverLoadSimulation extends Simulation {

  private val baseUrl: String = ConfigUtil.getFromConfig("baseUri")

  private val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
  private val request = http("Job run request")
    .get("/job/run")
    .queryParam("image", "${image}")
  private val overLoadScenario: ScenarioBuilder = scenario("Job Run Simulation")
    .exec(_.set("image", "library/hello-world:latest"))
    .exec(request)

  setUp(
    overLoadScenario.inject(atOnceUsers(6))
  )
    .protocols(httpProtocol)
    .assertions(
      global.responseTime.mean.lt(5000),
      global.responseTime.min.lt(500),
      global.failedRequests.count.is(1)
    )
}
