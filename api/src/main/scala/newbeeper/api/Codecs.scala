package newbeeper.api

import io.circe.{Decoder, Encoder}
import shapeless.Unwrapped

trait Codecs {

  implicit def decodeAnyVal[T, U](
      implicit
      ev: T <:< AnyVal,
      unwrapped: Unwrapped.Aux[T, U],
      decoder: Decoder[U]): Decoder[T] = Decoder.instance[T] { cursor =>
    val _ = ev
    decoder(cursor).map(unwrapped.wrap)
  }

  implicit def encodeAnyVal[T, U](
      implicit
      ev: T <:< AnyVal,
      unwrapped: Unwrapped.Aux[T, U],
      encoder: Encoder[U]): Encoder[T] = Encoder.instance[T] { value =>
    val _ = ev
    encoder(unwrapped.unwrap(value))
  }
}

object Codecs extends Codecs
