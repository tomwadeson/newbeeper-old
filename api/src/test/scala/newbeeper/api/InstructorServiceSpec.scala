package newbeeper.api

import cats.effect.IO
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import newbeeper.api.InstructorService.InstructorForm
import newbeeper.core.{InMemoryInstructorRepo, Instructor}
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.{EntityDecoder, Method, Request, Status, Uri}
import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.ScalacheckShapeless._
import org.scalatest.FreeSpec
import org.scalatest.prop.PropertyChecks

class InstructorServiceSpec extends FreeSpec with PropertyChecks with Codecs {

  "POST to /instructors should create an instructor" in {

    val request = Request[IO](method = Method.POST)

    forAll { instructorForm: InstructorForm =>
      val (response, created) =
        withClient { client =>
          for {
            (response, created) <- client.fetch(request.withEntity(instructorForm.asJson))(res =>
                                    res.as[Instructor[Instructor.Id]].tupleLeft(res))
          } yield (response, created)
        }.unsafeRunSync()

      assert(instructorForm == created.copy(id = ()))
      assert(response.status == Status.Created)
    }
  }

  "GET to /instructors should retrieve all instructors" in {

    val createInstructorReq = Request[IO](method = Method.POST)

    forAll { instructorForms: List[InstructorForm] =>
      val (created, retrieved) =
        withClient { client =>
          for {
            creationAttempts <- instructorForms
                                 .traverse(
                                   i =>
                                     client
                                       .fetch(createInstructorReq.withEntity(i.asJson))(res =>
                                         res.as[Instructor[Instructor.Id]])
                                       .attempt)
            created = creationAttempts collect { case Right(i) => i }
            retrieved <- client.fetchAs[List[Instructor[Instructor.Id]]](Request[IO]())
          } yield (created, retrieved)
        }.unsafeRunSync()

      assert(created == retrieved)
    }
  }

  "GET to /instructors/$id" - {

    "should retrieve the instructor when they exist" in {
      val createInstructorReq = Request[IO](method = Method.POST)

      forAll { instructorForm: InstructorForm =>
        val (created, retrieved) =
          withClient { client =>
            for {
              created <- client.fetchAs[Instructor[Instructor.Id]](
                          createInstructorReq.withEntity(instructorForm.asJson))
              retrieved <- client.expect[Instructor[Instructor.Id]](s"/${created.id.value}")
            } yield (created, retrieved)
          }.unsafeRunSync()

        assert(created == retrieved)
      }
    }

    "should return a 404 otherwise" in {
      forAll(Gen.choose(0, 100).map(_.toString)) { id: String =>
        val status =
          withClient { client =>
            client.get(s"/$id")(r => IO.pure(r.status))
          }.unsafeRunSync()

        assert(status == Status.NotFound)
      }
    }
  }

  "PATCH to /instructors/$id" - {

    val createInstructorReq = Request[IO](method = Method.POST)
    val patchInstructorReq = Request[IO](method = Method.PATCH)

    "should update the instructor when they exist" in {

      forAll { (instructorForm: InstructorForm, patchForm: InstructorPatch) =>
        val (created, updated, retrieved) =
          withClient { client =>
            for {
              created <- client.fetchAs[Instructor[Instructor.Id]](
                          createInstructorReq.withEntity(instructorForm.asJson))
              updated <- client.fetchAs[Instructor[Instructor.Id]](
                          patchInstructorReq
                            .withUri(Uri.unsafeFromString(s"/${created.id.value}"))
                            .withEntity(patchForm.asJson))
              retrieved <- client.expect[Instructor[Instructor.Id]](s"/${created.id.value}")
            } yield (created, updated, retrieved)
          }.unsafeRunSync()

        assert(updated.id == created.id)
        assert(updated == retrieved)
        assert(patchForm.email.forall(_ == updated.email))
        assert(patchForm.name.forall(_ == updated.name))
      }
    }

    "should return a 404 otherwise" in {
      forAll(Gen.choose(0, 100).map(_.toString), arbitrary[InstructorPatch]) {
        (id: String, patchForm: InstructorPatch) =>
          val status =
            withClient { client =>
              client.fetch(
                patchInstructorReq
                  .withUri(Uri.unsafeFromString(s"/$id"))
                  .withEntity(patchForm.asJson))(r => IO.pure(r.status))
            }.unsafeRunSync()

          assert(status == Status.NotFound)
      }
    }
  }

  "DELETE to /instructors/$id should delete the instructor when they exist" in {

    val getReq = Request[IO]()
    val createReq = Request[IO](method = Method.POST)
    val deleteReq = Request[IO](method = Method.DELETE)

    forAll { instructorForm: InstructorForm =>
      val (retrievedPreDelete, retrievedPostDelete) =
        withClient { client =>
          for {
            created <- client.fetchAs[Instructor[Instructor.Id]](
                        createReq.withEntity(instructorForm.asJson))
            retrievedPreDelete <- client.status(
                                   getReq.withUri(Uri.unsafeFromString(s"/${created.id.value}")))
            _ <- client.status(deleteReq.withUri(Uri.unsafeFromString(s"/${created.id.value}")))
            retrievedPostDelete <- client.status(
                                    getReq.withUri(Uri.unsafeFromString(s"/${created.id.value}")))
          } yield (retrievedPreDelete, retrievedPostDelete)
        }.unsafeRunSync()

      assert(retrievedPreDelete == Status.Ok)
      assert(retrievedPostDelete == Status.NotFound)
    }
  }

  def withClient[A](f: Client[IO] => IO[A]): IO[A] =
    for {
      instructorRepo <- InMemoryInstructorRepo[IO]
      instructorService = new InstructorService(instructorRepo)
      client = Client.fromHttpApp(instructorService.routes.orNotFound)
      result <- f(client)
    } yield result

  implicit val instructorDecoder: EntityDecoder[IO, Instructor[Instructor.Id]] =
    jsonOf[IO, Instructor[Instructor.Id]]

  implicit val instructorsDecoder: EntityDecoder[IO, List[Instructor[Instructor.Id]]] =
    jsonOf[IO, List[Instructor[Instructor.Id]]]
}
