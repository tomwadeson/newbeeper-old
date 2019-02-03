package newbeeper.api

import cats.data.{Kleisli, OptionT}
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{AuthedService, HttpApp, HttpRoutes, Request}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val config =
      pureconfig.loadConfigOrThrow[Environment]

    BlazeServerBuilder[IO]
      .bindHttp(config.http.port.value, config.http.hostname.value)
      .withHttpApp(routes)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
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

  val routes: HttpApp[IO] =
    Router(
      "/" -> (helloWorldService <+> Auth.middleware(secretMessageService))
    ).orNotFound
}

object Auth {
  val authUser: Kleisli[OptionT[IO, ?], Request[IO], User] =
    Kleisli(_ => OptionT.liftF(IO.pure(User(1, "Tom"))))

  val middleware: AuthMiddleware[IO, User] =
    AuthMiddleware(authUser)
}

final case class User(id: Long, name: String)
