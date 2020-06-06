import cats.effect._
import config.Config
import db.Database
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig}
import repository.UserRepository
import service.{RedisService, RoomRouteService, RoomService}

import scala.concurrent.duration._

object HttpServer {
    def create(configFile: String = "application.conf")(implicit contextShift: ContextShift[IO], concurrentEffect: ConcurrentEffect[IO], timer: Timer[IO]): IO[ExitCode] = {
        resources(configFile).use(create)
    }

    private def resources(configFile: String)(implicit contextShift: ContextShift[IO]): Resource[IO, Resources] = {
        for {
            config <- Config.load(configFile)
            ec <- ExecutionContexts.fixedThreadPool[IO](config.databaseConfig.threadPoolSize)
            blocker <- Blocker[IO]
            transactor <- Database.transactor(config.databaseConfig, ec, blocker)
        } yield Resources(transactor, config)
    }

    private def create(resources: Resources)(implicit concurrentEffect: ConcurrentEffect[IO], timer: Timer[IO]): IO[ExitCode] = {

        val methodConfig = CORSConfig(
            anyOrigin = true,
            anyMethod = false,
            allowedMethods = Some(Set("GET", "POST")),
            // allowedOrigins = Set("*"),
            allowCredentials = false,
            maxAge = 1.day.toSeconds)

        for {
            _ <- Database.initialize(resources.transactor)
            repository = new UserRepository(resources.transactor)
            redis = new RedisService(resources.config.redisConfig)
            gameService = new RoomService(repository, resources.config.gameProviderConfig, resources.config.botConfig, redis)
            exitCode <- BlazeServerBuilder[IO]
                .bindHttp(resources.config.serverConfig.port, resources.config.serverConfig.host)
                .withHttpApp(CORS(new RoomRouteService(gameService).routes, methodConfig).orNotFound).serve.compile.lastOrError
        } yield exitCode
    }

    case class Resources(transactor: HikariTransactor[IO], config: Config)

}
