# syntax=docker/dockerfile:1.7

# ---- Stage 1: build everything (sbt + node) ------------------------------
FROM sbtscala/scala-sbt:eclipse-temurin-21.0.5_11_1.10.7_3.6.2 AS builder

# Install Node 20 for the Vite build.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates gnupg \
 && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Cache sbt plugin / build deps before copying full sources.
COPY project/build.properties project/build.properties
COPY project/plugins.sbt    project/plugins.sbt
COPY build.sbt              build.sbt
RUN sbt update

# Bring in the full source tree.
COPY api      api
COPY shared   shared
COPY server   server
COPY client   client
# Cortex markdown content. Read by the server at /api/cortex/*.
COPY content  content

# Build the backend (creates server/target/universal/stage with launcher) and
# emit the linked Scala.js module. fullLinkJS produces the optimised JS that
# Vite then imports via the @scala-js/vite-plugin-scalajs plugin.
RUN sbt "server/Universal/stage" "client/fullLinkJS"

# Build the frontend bundle.
WORKDIR /build/client
RUN npm install --no-audit --no-fund \
 && npm run build

# ---- Stage 2: runtime image (JRE only) -----------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Server distribution: bin/cortex-server, lib/*.jar
COPY --from=builder /build/server/target/universal/stage /app
# Frontend bundle: served by the zio-http static fallback.
COPY --from=builder /build/client/dist /app/static
# Cortex content: read by the server at /api/cortex/*.
COPY --from=builder /build/content     /app/content

ENV STATIC_DIR=/app/static
ENV CORTEX_ROOT=/app/content/cortex
ENV PORT=8080
EXPOSE 8080

CMD ["bin/cortex-server"]
