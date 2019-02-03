package newbeeper.api

final case class Environment(http: Environment.Http)

object Environment {

  final case class Http(hostname: Http.Hostname, port: Http.Port)

  object Http {

    final case class Hostname(value: String) extends AnyVal

    final case class Port(value: Int) extends AnyVal
  }
}
