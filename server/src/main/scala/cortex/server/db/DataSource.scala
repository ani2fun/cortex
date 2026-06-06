package cortex.server.db

import cortex.server.config.DbConfig
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import zio.*

import javax.sql.DataSource as JDataSource

object DataSource:

  val live: ZLayer[DbConfig, Throwable, JDataSource] =
    ZLayer.scoped {
      for
        cfg <- ZIO.service[DbConfig]
        ds <- ZIO.acquireRelease(
          ZIO.attemptBlocking {
            val hc: HikariConfig = HikariConfig()
            hc.setJdbcUrl(cfg.url)
            hc.setUsername(cfg.user)
            hc.setPassword(cfg.password)
            hc.setMaximumPoolSize(10)
            hc.setPoolName("cortex-pool")
            HikariDataSource(hc)
          }
        )(ds => ZIO.attempt(ds.close()).orDie)
      yield ds: JDataSource
    }
