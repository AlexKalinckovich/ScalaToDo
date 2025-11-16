package db.migrations

import cats.effect.IO
import config.MongoConfig
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.ext.mongodb.database.MongoConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.Contexts
import java.util.Properties

object MongoMigrations {

    def run(config: MongoConfig): IO[Unit] = {
        IO.blocking {
            var liquibase: Liquibase = null

            try {
                val database = DatabaseFactory.getInstance().openDatabase(
                    config.uri,
                    null, 
                    null,  
                    null, 
                    null, 
                    null, 
                    null, 
                    new ClassLoaderResourceAccessor()
                )


                liquibase = new Liquibase(
                    "db/changelog/mongo/changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    database
                )

                println("Running MongoDB migrations...")
                liquibase.update(new Contexts())
                println("MongoDB migrations run successfully.")

            } catch {
                case e: Exception =>
                    println(s"Error running MongoDB migrations: ${e.getMessage}")
                    e.printStackTrace()
                    throw e
            } finally {
                if (liquibase != null) {
                    liquibase.close()
                }
            }
        }
    }
}