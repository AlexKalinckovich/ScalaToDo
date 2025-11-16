package config

import cats.effect.IO
import ciris.*
import ciris.http4s.*
import com.comcast.ip4s.Port

case class DbConfig(
                       url: String,
                       user: String,
                       pass: String
                   )

case class MongoConfig(
                          uri: String
                      )

case class AppConfig(
                        db: DbConfig,
                        mongo: MongoConfig,
                        httpPort: Port
                    )

object Config {

    def load(): IO[AppConfig] = {

        val configLoader = for {
            dbUrl    <- env("DB_URL").as[String]
            dbUser   <- env("DB_USER").as[String]
            dbPass   <- env("DB_PASS").as[String]

            mongoUri <- env("MONGO_URI").as[String]

            httpPort <- env("HTTP_PORT").as[Port]
        } yield AppConfig(
            DbConfig(dbUrl, dbUser, dbPass),
            MongoConfig(mongoUri),
            httpPort
        )

        configLoader.load[IO]
    }
}