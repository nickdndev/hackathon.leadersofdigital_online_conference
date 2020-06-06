import cats.effect.{Blocker, ContextShift, IO, Resource}
import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

package object config {

    case class ServerConfig(host: String, port: Int)

    case class DatabaseConfig(driver: String, url: String, user: String, password: String, threadPoolSize: Int)

    case class GameConfig(user: String, password: String, server: String)

    case class RedisConfig(host: String, port: Int, threads: Int)

    case class BotConfig(id: Int)

    case class Config(serverConfig: ServerConfig,
                      databaseConfig: DatabaseConfig,
                      gameProviderConfig: GameConfig,
                      redisConfig: RedisConfig,
                      botConfig: BotConfig)

    object Config {
        def load(configFile: String = "config/application.conf")(implicit cs: ContextShift[IO]): Resource[IO, Config] = {
            Blocker[IO].flatMap { blocker =>
                Resource.liftF(ConfigSource.fromConfig(ConfigFactory.load()).loadF[IO, Config](blocker))
            }
        }
    }

}
