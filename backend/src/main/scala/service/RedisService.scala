package service

import java.util.concurrent.{Executors, TimeUnit}

import cats.effect.IO
import config.RedisConfig
import io.netty.channel.nio.NioEventLoopGroup
import org.redisson.Redisson
import org.redisson.config.Config

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.util.{Failure, Success, Try}

class RedisService(redisConfig: RedisConfig) {

    private implicit val executorService: ExecutionContextExecutorService =
        scala.concurrent.ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(redisConfig.threads))

    private val commonEventLoopGroup = new NioEventLoopGroup()

    private val ROOM_STORE_KEY: String = "ROOM_STORE"
    private val ROOM_SESSION_KEY: String = "ROOM_SESSION"

    private val redisClient = Try {
        val config = new Config()
            .setEventLoopGroup(commonEventLoopGroup)
            .setCodec(new org.redisson.codec.SerializationCodec(getClass.getClassLoader))
            .setExecutor(executorService)
        val singleServerConfig = config.useSingleServer()
            .setAddress(s"${redisConfig.host}:${redisConfig.port}")
            .setTimeout(30000)
            .setConnectionMinimumIdleSize(4)
            .setConnectionPoolSize(4)
            .setSubscriptionConnectionPoolSize(1024)
            .setSubscriptionsPerConnection(128)
        // password.foreach(singleServerConfig.setPassword)
        //Logger.info(s"Creating new redisson client for ${singleServerConfig.getAddress}")
        Redisson.create(config)
    } match {
        case Success(client) =>
            /*lifecycle.addStopHook(() => Future {
                Logger.info("Close current redisson connection on application shutdown")
                client.shutdown()
            }(executorService))*/
            client
        case Failure(e) =>
            //Logger.error("Get an critical error on creation new redisson connection on application startup", e)
            throw new ExceptionInInitializerError("Get an critical error on creation new redisson connection on application startup")
    }


    def saveLatestGame(userId: Long, gameId: String): IO[Long] =
        IO {
            redisClient.getMapCache[String, Long](s"${ROOM_STORE_KEY}_$userId")
                .put(gameId, System.currentTimeMillis(), 30, TimeUnit.DAYS)
        }

    def findLatestGames(userId: Long)(implicit executionContext: ExecutionContext): IO[List[String]] =
        IO {
            redisClient.getMapCache[String, Long](s"${ROOM_STORE_KEY}_$userId").entrySet().asScala
                .toList.sortWith(_.getValue > _.getValue).take(3)
                .map(_.getKey)
        }


    def saveSession(userId: Long, sessionId: String, botId: Int): IO[String] = {
        IO {
            redisClient.getMapCache[String, String](ROOM_SESSION_KEY)
                .put(sessionId, s"${botId}_$userId", 3, TimeUnit.HOURS)
        }
    }
}
