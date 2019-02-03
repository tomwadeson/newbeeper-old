package newbeeper.api

import cats.effect.Sync
import org.http4s.server.Router
import org.http4s.{HttpApp, HttpRoutes}
import cats.implicits._
import org.http4s.implicits._

final case class Routes[F[_]](
    helloWorldService: HttpRoutes[F],
    secretMessageService: HttpRoutes[F]) {

  def httpApp(implicit F: Sync[F]): HttpApp[F] =
    Router(
      "/" -> (helloWorldService <+> secretMessageService)
    ).orNotFound
}
