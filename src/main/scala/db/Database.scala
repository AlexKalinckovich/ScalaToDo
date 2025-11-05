package db

import cats.effect.{IO, Resource}
import config.DbConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Database {

    private val THREAD_POOL_SIZE : Int =  32
    
    def transactor(dbConfig: DbConfig): Resource[IO, HikariTransactor[IO]] = {
        for {
            ec <- ExecutionContexts.fixedThreadPool[IO](THREAD_POOL_SIZE)
            xa <- HikariTransactor.newHikariTransactor[IO](
                driverClassName = "org.postgresql.Driver",
                url = dbConfig.url,
                user = dbConfig.user,
                pass = dbConfig.pass,
                connectEC = ec
            )
        } yield xa
    }
}