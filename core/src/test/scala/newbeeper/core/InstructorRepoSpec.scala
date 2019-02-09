package newbeeper.core

import cats.effect.{Effect, IO}
import cats.implicits._
import newbeeper.core.InstructorError.EmailAddressAlreadyInUse
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FreeSpec, OptionValues}
import org.scalacheck.ScalacheckShapeless._

abstract class InstructorRepoSpec[F[_]](implicit F: Effect[F])
    extends FreeSpec
    with PropertyChecks
    with OptionValues {

  "create and findById" - {

    "should create a new instructor when their email address does not exist" in {

      forAll { instructor: Instructor[Unit] =>
        val (created, lookedUp) =
          withInstructorRepo { instructorRepo =>
            for {
              created <- instructorRepo.create(instructor)
              lookedUp <- instructorRepo.findById(created.id)
            } yield (created, lookedUp)
          }.unsafeRunSync

        assert(lookedUp.value == created)
      }
    }

    "should fail when an email address already exists" in {

      forAll { instructor: Instructor[Unit] =>
        val error =
          withInstructorRepo { instructorRepo =>
            (for {
              _ <- instructorRepo.create(instructor)
              _ <- instructorRepo.create(instructor)
            } yield ()).attempt
          }.unsafeRunSync

        assert(error == Left(EmailAddressAlreadyInUse(instructor.email)))
      }
    }
  }

  "findByEmail" - {

    "should return the instructor associated with an email address" in {

      forAll { instructor: Instructor[Unit] =>
        val (created, lookedUp) =
          withInstructorRepo { instructorRepo =>
            for {
              created <- instructorRepo.create(instructor)
              lookedUp <- instructorRepo.findByEmail(instructor.email)
            } yield (created, lookedUp)
          }.unsafeRunSync

        assert(lookedUp.value == created)
      }
    }

    "should return none when the email address is not associated with an instructor" in {

      forAll { instructor: Instructor[Unit] =>
        val lookedUp =
          withInstructorRepo { instructorRepo =>
            for {
              _ <- instructorRepo.create(instructor)
              lookedUp <- instructorRepo.findByEmail(s"${instructor.email}-unknown")
            } yield lookedUp
          }.unsafeRunSync

        assert(lookedUp.isEmpty)
      }
    }
  }

  "all" - {

    "should retrieve all created instructors" in {

      forAll { instructors: List[Instructor[Unit]] =>
        val (created, retrieved) =
          withInstructorRepo { instructorRepo =>
            for {
              creationAttempts <- instructors.traverse(i => instructorRepo.create(i).attempt)
              created = creationAttempts collect { case Right(i) => i }
              retrieved <- instructorRepo.all.compile.toList
            } yield (created, retrieved)
          }.unsafeRunSync

        assert(retrieved == created)
      }
    }
  }

  "update" - {

    import shapeless.poly._

    object f extends (Instructor ~> Instructor) {
      override def apply[T](f: Instructor[T]): Instructor[T] =
        f.copy(
          name = s"${f.name}${f.name}",
          email = s"${f.email}!!!"
        )
    }

    "should apply an update function when an instructor exists" in {

      forAll { instructor: Instructor[Unit] =>
        val updated =
          withInstructorRepo { instructorRepo =>
            for {
              created <- instructorRepo.create(instructor)
              updated <- instructorRepo.update(created.id, f)
            } yield updated
          }.unsafeRunSync

        assert(updated.value.copy(id = ()) == f(instructor))
      }
    }

    "should persist updates" in {

      forAll { instructor: Instructor[Unit] =>
        val (updated, found) =
          withInstructorRepo { instructorRepo =>
            for {
              created <- instructorRepo.create(instructor)
              updated <- instructorRepo.update(created.id, f)
              found <- instructorRepo.findById(created.id)
            } yield (updated, found)
          }.unsafeRunSync

        assert(updated.value == found.value)
      }
    }

    "should return none when an instructor does not exist" in {

      forAll { id: Instructor.Id =>
        val updated =
          withInstructorRepo { instructorRepo =>
            for {
              updated <- instructorRepo.update(id, f)
            } yield updated
          }.unsafeRunSync

        assert(updated.isEmpty)
      }
    }
  }

  def instructorRepoFactory: F[InstructorRepo[F]]

  def withInstructorRepo[A](f: InstructorRepo[F] => F[A]): F[A] =
    instructorRepoFactory.flatMap(f)
}

class InMemoryInstructorRepoSpec extends InstructorRepoSpec[IO] {

  val instructorRepoFactory: IO[InstructorRepo[IO]] =
    InMemoryInstructorRepo[IO]
}
