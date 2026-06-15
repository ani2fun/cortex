package cortex.server.http

import zio.test.*

/**
 * Pins what `/api/auth/config` advertises to the SPA as the tutor base URL.
 *
 * cortex-tutor is now a ClusterIP with no public Ingress, reached only through this backend's same-origin
 * reverse proxy ([[TutorProxyRoutes]]). So the config endpoint must advertise the RELATIVE proxy path
 * `"/tutor"` — never the raw in-cluster DNS (`AppConfig.tutorBaseUrl`), which the browser can't resolve and
 * which stays server-side as the proxy target. When no tutor is configured it must stay `None` so the coach
 * UI degrades. `ApiRoutes.advertisedTutorBaseUrl` owns that mapping; this spec covers it directly.
 */
object ApiRoutesSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Any] = suite("ApiRoutes.advertisedTutorBaseUrl")(
    test("a configured internal tutor DNS is advertised to the SPA as the same-origin path \"/tutor\"") {
      val advertised =
        ApiRoutes.advertisedTutorBaseUrl(Some("http://cortex-tutor.apps-prod.svc.cluster.local"))
      assertTrue(advertised.contains("/tutor"))
    },
    test("the raw internal DNS is never leaked to the browser") {
      val advertised =
        ApiRoutes.advertisedTutorBaseUrl(Some("http://cortex-tutor.apps-prod.svc.cluster.local"))
      assertTrue(!advertised.exists(_.contains("svc.cluster.local")))
    },
    test("no tutor configured -> None, so the coach UI degrades") {
      assertTrue(ApiRoutes.advertisedTutorBaseUrl(None).isEmpty)
    }
  )
