package codefolio.server.db

import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import zio.*

import javax.sql.DataSource as JDataSource

object Migrations:

  private val ChangelogPath = "db/changelog/db.changelog-master.yaml"

  val run: ZIO[JDataSource, Throwable, Unit] =
    ZIO.serviceWithZIO[JDataSource] { ds =>
      ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          val database  = DatabaseFactory.getInstance.findCorrectDatabaseImplementation(JdbcConnection(conn))
          val liquibase = Liquibase(ChangelogPath, ClassLoaderResourceAccessor(), database)
          liquibase.update("")
        finally conn.close()
      } *> ZIO.logInfo("Liquibase migrations applied")
    }
