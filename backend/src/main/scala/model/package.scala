package object model {

    abstract sealed class Importance(val value: String)

    case object High extends Importance("high")

    case object Medium extends Importance("medium")

    case object Low extends Importance("low")

    object Importance {
        private def values = Set(High, Medium, Low)

        def unsafeFromString(value: String): Importance = {
            values.find(_.value == value).get
        }
    }

    case class Todo(id: Option[Long], description: String, importance: Importance)

    case class GameExternalId(id: String, externalId: String)

    case class Game(id: String, title: String, description: String)

    case class GamePage(games: List[Game], pageSize: Int, total: Int)

    case class GamesPerTag(tagId: Int, games: List[Game])

    case class StartedGames(recentGames: List[Game], games: List[GamesPerTag])

    case class GamesRequest(page: Int, pageSize: Int, category: String, lang: String, tags: Option[List[Int]])

    case class StartedGamesRequest(lang: String, category:String, userId:Long)

    case class SessionRequest(gameId: String, userId: Long, isDemo: Boolean)

    case class GameTag(id: String, name: String)

    case object TodoNotFoundError

    case object GamesNotFoundError

    case class NewGameResponse[T](status: Int, response: T)

    case class NewGameSession(gameName: String,
                              gameAlias: String,
                              game_url: String,
                              token: String,
                              game_api: String,
                              game_base: String)

}
