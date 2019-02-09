package newbeeper.core

import cats.MonadError
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2.Stream
import newbeeper.core.Instructor.Id
import newbeeper.core.InstructorError.EmailAddressAlreadyInUse
import shapeless.poly.~>

final case class Instructor[A](id: A, name: String, email: String)

object Instructor {

  final case class Id(value: String) extends AnyVal
}

trait InstructorRepo[F[_]] {

  def create(instructor: Instructor[Unit]): F[Instructor[Id]]

  def findByEmail(email: String): F[Option[Instructor[Id]]]

  def findById(id: Id): F[Option[Instructor[Id]]]

  def all: Stream[F, Instructor[Id]]

  def update(id: Id, f: Instructor ~> Instructor): F[Option[Instructor[Id]]]

  def delete(id: Id): F[Unit]
}

sealed trait InstructorError extends Throwable

object InstructorError {

  final case class EmailAddressAlreadyInUse(email: String) extends InstructorError
}

final class InMemoryInstructorRepo[F[_]] private (
    instructors: Ref[F, Map[Id, Instructor[Id]]],
    idSequence: Ref[F, Long])(implicit F: MonadError[F, Throwable])
    extends InstructorRepo[F] {

  override def create(instructor: Instructor[Unit]): F[Instructor[Id]] =
    for {
      exists <- findByEmail(instructor.email)
      _ <- exists.fold(F.unit)(_ => F.raiseError[Unit](EmailAddressAlreadyInUse(instructor.email)))
      id <- nextId
      i = instructor.copy[Id](id = id)
      _ <- instructors.update(_ + (id -> i))
    } yield i

  override def findByEmail(email: String): F[Option[Instructor[Id]]] =
    instructors.get.map(_.values.find(_.email == email))

  override def findById(id: Id): F[Option[Instructor[Id]]] =
    instructors.get.map(_.get(id))

  override def all: Stream[F, Instructor[Id]] =
    Stream
      .eval(instructors.get.map(_.values.toList.sortBy(_.id.value.toLong)))
      .flatMap(Stream.emits)

  override def update(id: Id, f: Instructor ~> Instructor): F[Option[Instructor[Id]]] =
    instructors.modify { is =>
      val maybeInstructor = is.get(id).map(i => f(i))
      (maybeInstructor.map(i => is.updated(id, i)).getOrElse(is), maybeInstructor)
    }

  override def delete(id: Id): F[Unit] =
    instructors.update(_ - id)

  private def nextId: F[Instructor.Id] =
    idSequence.modify(i => (i + 1, Id(i.toString)))
}

object InMemoryInstructorRepo {

  def apply[F[_]: Sync]: F[InMemoryInstructorRepo[F]] =
    for {
      instructors <- Ref[F].of(Map.empty[Id, Instructor[Id]])
      idSequence <- Ref[F].of(0L)
    } yield new InMemoryInstructorRepo(instructors, idSequence)
}
