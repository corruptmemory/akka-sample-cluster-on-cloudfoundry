package sample.cluster.factorial

import java.net.NetworkInterface

import scala.collection.JavaConversions._
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scalaj.http.{Http, HttpOptions}

class NetworkConfig(registryService: String, registryServiceTimeout: Int) {

  def hostLocalAddress: String =
    NetworkInterface.getNetworkInterfaces.
        find(_.getName equals "eth0").
        flatMap(interface =>
          interface.getInetAddresses.find(_.isSiteLocalAddress).map(_.getHostAddress)).
        getOrElse("127.0.0.1")

  def seedsConfig(
                   config: Config,
                   clusterName: String,
                   defaultIp: String,
                   defaultPort: Int): Config = {
      val backendInstances = queryServiceInstances match {
        case l if l.size >= 2 => l
        case l => l ++ Seq(s"$defaultIp:$defaultPort")
      }
      ConfigFactory.empty().withValue("akka.cluster.seed-nodes",
        ConfigValueFactory.fromIterable(backendInstances.map(ipPort => s"akka.tcp://$clusterName@$ipPort")))
    }

  def queryServiceInstances: Seq[String] = {
    Try(Http(s"http://$registryService/api/v1/instances?service_name=cluster-seed")
      .option(HttpOptions.readTimeout(registryServiceTimeout))).map(_.asString).map(resp => {
      (Json.parse(resp.body) \\ "value").map(_.as[String])
    }).getOrElse(Seq.empty[String])
  }

  def registerService(ip: String, port: Int)(implicit context: ExecutionContext): Future[String] = {
    Future(Http(s"http://$registryService/api/v1/instances")
      .postData(
        s"""{
          |"service_name": "cluster-seed",
          |"endpoint": {"type": "tcp","value": "$ip:$port"},
          |"ttl": 30,
          |"status": "UP",
          |"tags": ["tag-test"],
          |"metadata": {}}""".stripMargin)
      .header("Content-Type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.readTimeout(registryServiceTimeout)).asString)
      .map(resp => (Json.parse(resp.body) \ "id").as[String])
  }

  def heartbeat(id: String)(implicit context: ExecutionContext): Future[String] = {
    Future(Http(s"http://$registryService/api/v1/instances/$id/heartbeat")
      .put("")
      .header("Content-Type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.readTimeout(registryServiceTimeout)).asString)
      .map(_.body)
  }
}
