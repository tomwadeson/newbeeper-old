package newbeeper.api

import cats.data.{Kleisli, OptionT}
import cats.effect.{ExitCode, IO, IOApp}
import newbeeper.core.InMemoryInstructorRepo
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{AuthedService, HttpRoutes, Request}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val config =
      pureconfig.loadConfigOrThrow[Environment]

    for {
      instructorRepo <- InMemoryInstructorRepo[IO]
      instructorService = new InstructorService(instructorRepo)

      httpApp = Routes(
        helloWorldService,
        Auth.middleware(secretMessageService),
        instructorService.routes
      ).httpApp

      _ <- BlazeServerBuilder[IO]
            .bindHttp(config.http.port.value, config.http.hostname.value)
            .withHttpApp(httpApp)
            .serve
            .compile
            .drain

    } yield ExitCode.Success
  }

  val helloWorldService: HttpRoutes[IO] = {

    val dsl = Http4sDsl[IO]
    import dsl._

    HttpRoutes.of[IO] {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name")
    }
  }

  val secretMessageService: AuthedService[User, IO] = {

    val dsl = Http4sDsl[IO]
    import dsl._

    AuthedService {
      case GET -> Root / "secret" as user =>
        Ok(s"I've got a secret for you, ${user.name}")
    }
  }
}

final case class User(id: Long, name: String)

object Auth {
  val authUser: Kleisli[OptionT[IO, ?], Request[IO], User] =
    Kleisli(_ => OptionT.liftF(IO.pure(User(1, "Tom"))))

  val middleware: AuthMiddleware[IO, User] =
    AuthMiddleware(authUser)
}
