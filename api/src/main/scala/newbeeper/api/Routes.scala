package newbeeper.api

import cats.effect.Sync
import org.http4s.HttpApp
import org.http4s.implicits._
import org.http4s.server.Router

final case class Routes[F[_]](instructorService: InstructorService[F]) {

  def httpApp(implicit F: Sync[F]): HttpApp[F] =
    Router(
      "/instructors" -> instructorService.routes
    ).orNotFound
}
