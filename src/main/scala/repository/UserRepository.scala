package repository

import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import doobie.util.fragment.Fragment.const
import doobie.util.transactor.Transactor
import model._
import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.postgres.pgisimplicits._
import doobie.implicits.legacy.instant._

import cats.data.NonEmptyList
import Fragments.{ in,andOpt,whereAndOpt,whereAnd }

class UserRepository(transactor: Transactor[IO]) {

    def totalGames(category: String, tags: List[Int]): IO[Int] = {
        val tagsFragmnet = tags.toNel.map(tgs => in(fr"tag_id", tgs))
        val whereAndF = whereAndOpt(fr"category = $category".some, fr"id = game_id".some,tagsFragmnet)

        (const("select count(distinct id ) from games, game_tags")++whereAndF)
            .query[Int].unique.transact(transactor)
    }

    def gameGamesByIds(lang: String, gameIds:List[String]): IO[List[Game]] =
        const(s"""select g.id, g.title_$lang as title, g.description_$lang as description, array_agg(gt.tag_id) as tags
                from games as g,
                 game_tags gt
                where g.id = gt.game_id
                 and g.id in (${gameIds.map(id=>s"\'$id\'").mkString(",")})
                group by g.id, g.title_$lang, g.description_$lang""")
            .query[Game].to[List].transact(transactor)

    def getGames(lang: String, page: Int, pageSize: Int, tags: List[Int], category: String): IO[List[Game]] = {
        val havingTags = tags match {
            case Nil => fr""
            case tgs => fr" having array_agg(gt.tag_id)@>array[$tgs]"
        }

        val limit = fr"offset ${page * pageSize} limit ${pageSize}"
        (const(
            s"""
              select g.id, g.title_$lang as title, g.description_$lang as description,array_agg(gt.tag_id) as tags
                           from (select * from games where category = '$category' order by title_$lang) as g,
                                game_tags gt
                           where g.id = gt.game_id
                           group by g.id,g.title_$lang,g.description_$lang
            """) ++ havingTags++limit).query[Game].to[List].transact(transactor)
    }

    def getTags(lang: String) = {
        const(s"select id, name_$lang as name from tags").query[GameTag].to[List].transact(transactor)
    }

    def getGameExternalId(id: String): IO[Option[String]] = {
        sql"select external_id from games where id=$id".query[String].option.transact(transactor)
    }

    def getRandomGame(lang: String, category: String): IO[List[Game]] = {
        const(
            s"""
               |select g.id, g.title_$lang as title, g.description_$lang as description,array_agg(gt.tag_id) as tags
               |              from (
               |                       select *
               |                       from games where category = '$category' order by title_$lang offset floor (random() * (select count(*) from games where category = '$category'))
               |                       limit 1) as g,
               |                   game_tags gt
               |              where g.id = gt.game_id
               |              group by g.id, g.title_$lang,g.description_$lang
            """.stripMargin).query[Game].to[List].transact(transactor)
    }

    def getStartedGames(lang: String, category: String): IO[List[(Int, List[Game])]] =
        for {
            tags <- sql"select id from tags".query[Int].to[List].transact(transactor)
            res <- tags.map(tagId => getGames(lang, 0, 7, List(tagId), category).map((tagId, _))).sequence
        } yield res

}
