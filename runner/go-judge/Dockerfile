# go-judge sandbox + cortex language toolchains.
#
# Base: criyle/go-judge (Debian 13 "trixie", binary /opt/go-judge, listens :5050).
# go-judge bundles NO compilers — the sandbox bind-mounts the container's
# /usr (incl. /usr/local), /bin, /lib, /lib64, /etc/alternatives READ-ONLY (see
# /opt/mount.yaml) and runs user code as uid 1536. CONSEQUENCES for this image:
#   * every toolchain MUST live under a mounted path — /usr or /usr/local — NEVER
#     /opt or /root (those are not mounted into the sandbox).
#   * binaries/libs must be world-readable/executable (sandbox user is uid 1536).
#   * runtime has NO network (go-judge isolates per submission) and writable dirs
#     are only /w (workDir) and /tmp (tmpfs) — both via HOME=/w at run time.
#
# Languages mirror Languages.scala (Judge0 ids): python(71) java(62) scala(81)
# c(50) c++(54) go(60) rust(73) kotlin(78) typescript(74) javascript(63) sql(82).
# JDK 21 (not 17): trixie dropped openjdk-17; mount.yaml already binds
# /etc/java-21-openjdk; the Visualise tracer uses javax.tools (version-agnostic).
FROM criyle/go-judge:latest

ENV DEBIAN_FRONTEND=noninteractive

# apt toolchains — all land in /usr (mounted). rustc via apt (NOT rustup, which
# installs to /root/.cargo — invisible to the sandbox). nodejs+npm: the base has
# no Node, needed for JS(63)/TS(74).
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl wget unzip \
      python3 \
      build-essential \
      golang \
      openjdk-21-jdk-headless \
      sqlite3 \
      rustc \
      nodejs npm \
 && rm -rf /var/lib/apt/lists/*

# TypeScript + tsx. Debian's npm global prefix is /usr/local → installs under
# /usr (mounted). tsx runs .ts directly.
RUN npm install -g typescript tsx && npm cache clean --force

# Kotlin compiler → /usr/local (NOT /opt). kotlinc is a JVM launcher; needs java
# (present) at run time, no network for a single-file compile.
ENV KOTLIN_VERSION=2.1.0
RUN curl -fsSL "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip" -o /tmp/kotlin.zip \
 && unzip -q /tmp/kotlin.zip -d /usr/local \
 && ln -sf /usr/local/kotlinc/bin/kotlinc /usr/local/bin/kotlinc \
 && ln -sf /usr/local/kotlinc/bin/kotlin  /usr/local/bin/kotlin \
 && chmod -R a+rX /usr/local/kotlinc \
 && rm /tmp/kotlin.zip

# scala-cli (glibc static binary) → /usr/local/bin.
# ⚠ Scala(81) caveat: scala-cli fetches the Scala toolchain via coursier at run
# time (network). go-judge sandboxes have NO network, so Scala is best-effort
# until its coursier cache is pre-warmed into a mounted, uid-1536-readable path
# (follow-up; DSA content is Python+Java, and Scala was already egress-limited
# on the prior Code Runner). The binary install itself is correct here.
ENV SCALA_CLI_VERSION=1.5.4
RUN ARCH="$(uname -m)" \
 && case "$ARCH" in \
      x86_64)  SCALA_ARCH=x86_64-pc-linux ;; \
      aarch64) SCALA_ARCH=aarch64-pc-linux ;; \
      *) echo "unsupported arch: $ARCH"; exit 1 ;; \
    esac \
 && curl -fsSL "https://github.com/VirtusLab/scala-cli/releases/download/v${SCALA_CLI_VERSION}/scala-cli-${SCALA_ARCH}.gz" -o /tmp/scala-cli.gz \
 && gunzip /tmp/scala-cli.gz \
 && mv /tmp/scala-cli /usr/local/bin/scala-cli \
 && chmod 755 /usr/local/bin/scala-cli

# Inherit ENTRYPOINT ["/opt/go-judge"], WORKDIR /opt, port 5050 from the base.
# Seccomp hardening + tuned default limits are layered in via deploy flags (Phase 1b).
