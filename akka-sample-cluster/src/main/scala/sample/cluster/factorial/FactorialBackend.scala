package sample.cluster.factorial

import scala.annotation.tailrec
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.scaladsl.Http
import akka.pattern.pipe

//#backend
class FactorialBackend extends Actor with ActorLogging {

  import context.dispatcher

  def receive = {
    case (n: Int) =>
      Future(factorial(n)) map { result => (n, result) } pipeTo sender()
  }

  def factorial(n: Int): BigInt = {
    @tailrec def factorialAcc(acc: BigInt, n: Int): BigInt = {
      if (n <= 1) acc
      else factorialAcc(acc * n, n - 1)
    }
    factorialAcc(BigInt(1), n)
  }

}
//#backend

object FactorialBackend {

  def main(args: Array[String]): Unit = {

    val port = if (args.isEmpty) 2551 else args(0).toInt

    val internalIp = NetworkConfig.hostLocalAddress//NetworkConfig.cloudFoundryIp.orElse(NetworkConfig.serviceInstanceIp).getOrElse("127.0.0.1")

    println(s"internalIp:$internalIp")
    println(s"hostLocalAddress:${NetworkConfig.hostLocalAddress}")

    val appConfig = ConfigFactory.load("factorial")
    val clusterName = appConfig.getString("clustering.name")

    val config = ConfigFactory.parseString("akka.cluster.roles = [backend]").
      withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$internalIp")).
      withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")).
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.bind-hostname=0.0.0.0")).
      withFallback(NetworkConfig.seedsConfig(appConfig, clusterName, internalIp, port)).
      withFallback(appConfig)

    val system = ActorSystem(clusterName, config)

    import system.dispatcher
    import scala.concurrent.duration._

    def sendHeartbeat(id: String): Unit = system.scheduler.scheduleOnce(25.seconds) {
      NetworkConfig.heartbeat(id).map(_ => sendHeartbeat(id))
    }

    NetworkConfig.registerService(internalIp, port).map(id => {
      system.actorOf(Props[FactorialBackend], name = "factorialBackend")
      //system.actorOf(Props[MetricsListener], name = "metricsListener")
      sendHeartbeat(id)
    })
  }
}