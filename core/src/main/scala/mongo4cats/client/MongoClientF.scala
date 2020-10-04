package mongo4cats.client

import cats.effect.{Resource, Sync}
import cats.implicits._
import mongo4cats.database.MongoDatabaseF
import org.mongodb.scala.connection.ClusterSettings
import org.mongodb.scala.{MongoClient, MongoClientSettings, ServerAddress}

final case class MongoServerAddress(host: String, port: Int)

import scala.jdk.CollectionConverters._

final class MongoClientF[F[_]: Sync] private(
    private val client: MongoClient
) {

  def getDatabase(name: String): F[MongoDatabaseF[F]] =
    Sync[F].delay(client.getDatabase(name)).flatMap(MongoDatabaseF.make)
}

object MongoClientF {
  def fromServerAddress[F[_]: Sync](serverAddresses: MongoServerAddress*): Resource[F, MongoClientF[F]] = {
    val servers = serverAddresses.map(s => new ServerAddress(s.host, s.port)).toList.asJava
    val settings = MongoClientSettings
      .builder()
      .applyToClusterSettings((builder: ClusterSettings.Builder) => builder.hosts(servers))
      .build()
    clientResource(MongoClient(settings))
  }

  def fromConnectionString[F[_]: Sync](connectionString: String): Resource[F, MongoClientF[F]] =
    clientResource(MongoClient(connectionString))

  private def clientResource[F[_]: Sync](client: => MongoClient): Resource[F, MongoClientF[F]] =
    Resource.fromAutoCloseable(Sync[F].delay(client)).map(c => new MongoClientF[F](c))
}