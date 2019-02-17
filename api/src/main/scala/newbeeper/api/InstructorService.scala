package newbeeper.api

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import newbeeper.core.{Instructor, InstructorRepo}
import org.http4s.{EntityDecoder, HttpRoutes}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless.PolyDefns.~>

final class InstructorService[F[_]: Sync](instructorRepo: InstructorRepo[F])
    extends Http4sDsl[F]
    with Codecs {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case req @ POST -> Root =>
      for {
        instructorForm <- req.as[Instructor[Unit]]
        instructor <- instructorRepo.create(instructorForm)
        response <- Created(instructor.asJson)
      } yield response

    case GET -> Root =>
      Ok(instructorRepo.all.compile.toList.map(_.asJson))

    case GET -> Root / id =>
      for {
        maybeInstructor <- instructorRepo.findById(Instructor.Id(id))
        response <- maybeInstructor.map(_.asJson).fold(NotFound())(Ok(_))
      } yield response

    case req @ PATCH -> Root / id =>
      for {
        patchForm <- req.as[InstructorPatch]
        maybeUpdated <- instructorRepo.update(
                         Instructor.Id(id),
                         InstructorService.updateWithPatch(patchForm))
        response <- maybeUpdated.map(_.asJson).fold(NotFound())(Ok(_))
      } yield response

    case DELETE -> Root / id =>
      Ok(instructorRepo.delete(Instructor.Id(id)))
  }

  implicit val instructorFormDecoder: EntityDecoder[F, Instructor[Unit]] =
    jsonOf[F, Instructor[Unit]]

  implicit val instructorPatchDecoder: EntityDecoder[F, InstructorPatch] =
    jsonOf[F, InstructorPatch]

}

object InstructorService {

  def updateWithPatch(patch: InstructorPatch): Instructor ~> Instructor =
    new ~>[Instructor, Instructor] {
      override def apply[A](f: Instructor[A]): Instructor[A] =
        f.copy(
          name = patch.name.getOrElse(f.name),
          email = patch.email.getOrElse(f.email)
        )
    }
}

final case class InstructorPatch(
    name: Option[String],
    email: Option[String]
)
