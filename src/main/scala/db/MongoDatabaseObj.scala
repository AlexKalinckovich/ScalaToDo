package db

import cats.effect.{IO, Resource}
import config.MongoConfig
import mongo4cats.client.MongoClient
import mongo4cats.database.MongoDatabase

object MongoDatabaseObj {

    private def clientResource(config: MongoConfig): Resource[IO, MongoClient[IO]] =
        MongoClient.fromConnectionString[IO](config.uri)

    def databaseResource(config: MongoConfig): Resource[IO, MongoDatabase[IO]] =
        clientResource(config).evalMap(_.getDatabase("tododb")) 
}