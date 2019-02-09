package newbeeper

import cats.effect.Effect

package object core {

  implicit class RichEffect[F[_], A](underlying: F[A]) {
    def unsafeRunSync(implicit F: Effect[F]): A =
      F.toIO(underlying).unsafeRunSync()
  }

}
