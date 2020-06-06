package service


import java.util.concurrent.Executors

import cats.data.OptionT
import cats.effect.{IO, _}
import config.{BotConfig, GameConfig}
import io.circe.generic.auto._
import model.{Game, _}
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, Uri}
import repository.UserRepository

import scala.concurrent.ExecutionContext.{fromExecutor, global}
import scala.concurrent.ExecutionContextExecutor

class RoomService(repository: UserRepository,
                  gameProvider: GameConfig,
                  botConfig: BotConfig,
                  redisService: RedisService) extends Http4sDsl[IO] {

    private implicit val pool: ExecutionContextExecutor = fromExecutor(Executors.newFixedThreadPool(5))
    private implicit val cs: ContextShift[IO] = IO.contextShift(global)
    private implicit val authResponseEntityDecoder: EntityDecoder[IO, NewGameResponse[NewGameSession]] = jsonOf[IO, NewGameResponse[NewGameSession]]

    private val gwUser: String = gameProvider.user
    private val gwPassword: String = gameProvider.password
    private val gwServer: String = gameProvider.server
    private val baseUri = Uri.unsafeFromString(s"$gwServer/rgs/api/admin").withQueryParam("usr", gwUser).withQueryParam("passw", gwPassword)


    def getTags(lang: String): IO[List[model.GameTag]] = repository.getTags(lang)

    def getGames(req: GamesRequest): IO[GamePage] = {
        for {
            total <- repository.totalGames(req.category, req.tags.getOrElse(List.empty))
            games <- repository.getGames(req.lang, req.page, req.pageSize, req.tags.getOrElse(List.empty), req.category)
        } yield GamePage(games, req.pageSize, total)

    }

    def getRandomGame(lang: String, category: String): IO[List[Game]] = repository.getRandomGame(lang, category)

    def getStartedGames(userId: Long, lang: String, category: String): IO[StartedGames] = {

        def recentGames: IO[List[Game]] =
            for {
                recentIds <- redisService.findLatestGames(userId)
                games <- recentIds match {
                    case Nil => IO.pure(List.empty)
                    case ids => repository.gameGamesByIds(lang, ids)
                }
            } yield games

        for {
            games <- repository.getStartedGames(lang, category).map(_.map(i => GamesPerTag(i._1, i._2)))
            recent <- recentGames
        } yield StartedGames(recent, games)
    }

    def createSession(userId: Long, gameId: String, isDemo: Boolean): OptionT[IO, String] = {
        def createSession(externalId: String, userId: Long): IO[NewGameResponse[NewGameSession]] = {
            val mode = if (isDemo) "create_demo_session" else "create_session"
            val req = baseUri
                .withQueryParam("action", mode)
                .withQueryParam("game_id", externalId)
                .withQueryParam("remote_id", userId)


            BlazeClientBuilder[IO](global).resource.use { client =>
                client.expect[NewGameResponse[NewGameSession]](req)
            }
        }

        for {
            externalId <- OptionT(repository.getGameExternalId(gameId))
            res <- OptionT.liftF(createSession(externalId, userId))
            _ <- OptionT.liftF(redisService.saveLatestGame(userId, gameId))
            _ <- OptionT.liftF(redisService.saveSession(userId, res.response.token, botConfig.id))
        } yield res.response.game_url
    }
}