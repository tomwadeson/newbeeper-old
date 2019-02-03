package newbeeper.api

import pureconfig.ConfigReader
import pureconfig.generic.auto._
import pureconfig.generic.semiauto._

final case class Environment(http: Environment.Http)

object Environment {

  final case class Http(hostname: Http.Hostname, port: Http.Port)

  object Http {

    final case class Hostname(value: String) extends AnyVal

    final case class Port(value: Int) extends AnyVal
  }

  implicit val environmentReader: ConfigReader[Environment] =
    deriveReader[Environment]
}
