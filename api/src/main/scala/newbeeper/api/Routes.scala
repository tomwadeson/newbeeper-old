package newbeeper.api

import cats.effect.Sync
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.{HttpApp, HttpRoutes}

final case class Routes[F[_]](instructorService: HttpRoutes[F]) {

  def httpApp(implicit F: Sync[F]): HttpApp[F] =
    Router(
      "/instructors" -> instructorService
    ).orNotFound
}
