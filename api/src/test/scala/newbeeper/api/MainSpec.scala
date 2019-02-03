package newbeeper.api

import org.scalatest.{FreeSpec, Matchers}
import org.http4s.client.Client
import org.http4s.implicits._

class MainSpec extends FreeSpec with Matchers {
  "/hello/$name" - {
    "should respond with 'Hello, $name'" in {
      val client = Client.fromHttpApp(Main.helloWorldService.orNotFound)
      val message = client.expect[String]("/hello/World!").unsafeRunSync()
      message shouldBe "Hello, World!"
    }
  }
}
