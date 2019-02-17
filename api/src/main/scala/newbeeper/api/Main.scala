package newbeeper.api

import cats.effect.{ExitCode, IO, IOApp}
import newbeeper.core.InMemoryInstructorRepo
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val config =
      pureconfig.loadConfigOrThrow[Environment]

    for {
      instructorRepo <- InMemoryInstructorRepo[IO]
      instructorService = new InstructorService(instructorRepo)

      httpApp = Routes(
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
}

final case class User(id: Long, name: String)
