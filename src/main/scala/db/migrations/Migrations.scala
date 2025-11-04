package db.migrations

import cats.effect.IO
import config.DbConfig
import liquibase.{Contexts, Liquibase}
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

import java.sql.DriverManager

object Migrations {

    def run(config: DbConfig): IO[Unit] = {

        IO.blocking {
            var connection: java.sql.Connection = null
            try {
                Class.forName("org.postgresql.Driver")
                connection = DriverManager.getConnection(
                    config.url,
                    config.user,
                    config.pass
                )

                val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                    new JdbcConnection(connection)
                )

                val liquibase = new Liquibase(
                    "db/changelog/changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    database
                )

                println("Running database migrations...")
                liquibase.update(new Contexts())
                println("Migrations run successfully.")

            } catch {
                case e: Exception =>
                    println(s"Error running migrations: ${e.getMessage}")
                    throw e
            } finally {
                if (connection != null) {
                    connection.close()
                }
            }
        }
    }
}