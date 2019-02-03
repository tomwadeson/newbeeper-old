package newbeeper.api

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpApp, HttpRoutes}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(routes)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

  val helloWorldService: HttpRoutes[IO] = {

    val dsl = Http4sDsl[IO]
    import dsl._

    HttpRoutes.of[IO] {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name")
    }
  }

  val routes: HttpApp[IO] =
    Router(
      "/" -> helloWorldService
    ).orNotFound
}
