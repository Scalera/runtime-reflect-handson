package org.scalera.reflect.runtime

import spray.json.DefaultJsonProtocol

import scala.util.Try
import DefaultJsonProtocol._

object case1 extends App {

  import utils._

  val fooSerializer: JsonSer[Foo] =
    jsonSer(jsonFormat2(Foo.apply))

  val barSerializer: JsonSer[Bar] =
    jsonSer(jsonFormat1(Bar.apply))

  val foo: Foo = Foo(1, "hi")

  lazy val consumer = new Consumer {
    override val consume: Any => Unit = {
      case message: String =>
        Seq(barSerializer, fooSerializer).flatMap { ser =>
          Try(ser.deserialize(message)).toOption
        }.headOption.fold(ifEmpty = println("Couldn't deserialize")) {
          case bar: Bar => println("it's a bar!")
          case foo: Foo => println("it's a foo!")
          case _ => println("it's ... something!")
        }
    }
  }

  lazy val producer = new Producer {
    override def produce(message: Any): Try[Unit] = Try(consumer.consume(message))
  }

  producer.produce(fooSerializer.serialize(foo))


}