package service

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import model._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class RoomRouteService(gameService: RoomService) extends Http4sDsl[IO] {
    private implicit val encodeImportance: Encoder[Importance] = Encoder.encodeString.contramap[Importance](_.value)

    private implicit val decodeImportance: Decoder[Importance] = Decoder.decodeString.map[Importance](Importance.unsafeFromString)

    val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
        case req@POST -> Root / "games" =>
            for {
                req <- req.decodeJson[GamesRequest]
                gamePage <- gameService.getGames(req)
                res <- Ok(gamePage.asJson)
            } yield res

        case GET -> Root / "game" / "tags" / lang => Ok(gameService.getTags(lang).map(_.asJson))

        case GET -> Root / "game" / "tags" / lang => Ok(gameService.getTags(lang).map(_.asJson))

        case GET -> Root / "game" / "random" / lang / category => Ok(gameService.getRandomGame(lang, category).map(_.asJson))

        case req@POST -> Root / "game" / "started" =>
            for {
                req <- req.decodeJson[StartedGamesRequest]
                gamePage <- gameService.getStartedGames(req.userId, req.lang, req.category).map(_.asJson)
                res <- Ok(gamePage.asJson)
            } yield res

        case req@POST -> Root / "game" / "link" =>
            for {
                req <- req.decodeJson[SessionRequest]
                res <- gameService.createSession(req.userId, req.gameId, req.isDemo).value
                    .flatMap {
                        case None => NotFound()
                        case Some(session) => Ok(session)
                    }
            } yield res

        case GET -> Root / "healthz" => Ok()
        case _ => Ok()
    }
}
