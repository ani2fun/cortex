package codefolio.server.http

import zio.test.*

object ClientIpSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Any] = suite("ClientIp.key")(
    test("prefers X-Real-IP when present") {
      assertTrue(ClientIp.key(Some("203.0.113.7"), Some("1.2.3.4, 203.0.113.7")) == "203.0.113.7")
    },
    test("falls back to the LAST X-Forwarded-For hop (the trusted-proxy entry)") {
      // The first hop is client-controlled — only the last hop, appended by the ingress, is trustworthy.
      assertTrue(ClientIp.key(None, Some("9.9.9.9, 8.8.8.8, 203.0.113.7")) == "203.0.113.7")
    },
    test("uses a single X-Forwarded-For hop as-is") {
      assertTrue(ClientIp.key(None, Some("203.0.113.7")) == "203.0.113.7")
    },
    test("trims surrounding whitespace") {
      assertTrue(ClientIp.key(Some("  203.0.113.7  "), None) == "203.0.113.7")
    },
    test("a blank X-Real-IP falls through to X-Forwarded-For") {
      assertTrue(ClientIp.key(Some("   "), Some("1.1.1.1, 203.0.113.7")) == "203.0.113.7")
    },
    test("no headers collapse onto the shared 'unknown' bucket") {
      assertTrue(ClientIp.key(None, None) == "unknown")
    },
    test("an all-blank X-Forwarded-For yields 'unknown'") {
      assertTrue(ClientIp.key(None, Some("  ,  ")) == "unknown")
    }
  )
