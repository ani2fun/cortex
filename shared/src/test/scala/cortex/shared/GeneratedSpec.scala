package cortex.shared

import cortex.shared.api.Endpoints.Greeting
import cortex.shared.api.EndpointsJsonSerdes.*
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import zio.test.*

object GeneratedSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Any] = suite("openapi codegen smoke test")(
    test("Greeting JSON round-trips through generated circe codecs") {
      val g       = Greeting(message = "hi", visits = 3L, cached = false)
      val json    = g.asJson.noSpaces
      val decoded = decode[Greeting](json)
      assertTrue(decoded == Right(g))
    }
  )
