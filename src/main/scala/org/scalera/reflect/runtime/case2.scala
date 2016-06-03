package org.scalera.reflect.runtime

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.util.Try
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.{TypeTag, typeTag}

//  Message wrapper
case class Message[T: TypeTag](content: T){
  val messageType: Message.Type = typeTag[T].tpe.toString
}
object Message {

  type Type = String

  def typeFrom(msg: String): Message.Type = {
    val JsString(tpe) = JsonParser(msg).asJsObject.fields("messageType")
    tpe
  }

  implicit def messageSer[T:TypeTag:JsonSer]: JsonSer[Message[T]] =
    new JsonSer[Message[T]] {
      def serialize(t: Message[T]): String = JsObject(
        "messageType" -> JsString(t.messageType),
        "msg" -> JsonParser(implicitly[JsonSer[T]].serialize(t.content))).prettyPrint
      def deserialize(json: String): Message[T] = {
        val fields = JsonParser(json).asJsObject.fields
        val JsString(tpe) = fields("messageType")
        val content = implicitly[JsonSer[T]].deserialize(fields("msg").prettyPrint)
        new Message[T](content) {
          override val messageType = tpe
        }
      }
    }
}

object case2 extends App {

  import utils._
  import Message._

  implicit val fooSerializer: JsonSer[Foo] =
    jsonSer(jsonFormat2(Foo.apply))

  implicit val barSerializer: JsonSer[Bar] =
    jsonSer(jsonFormat1(Bar.apply))

  val foo: Foo = Foo(1, "hi")

  lazy val consumer = new Consumer {
    override val consume: Any => Unit = {
      case message: String =>
        Message.typeFrom(message) match {

          case "org.scalera.reflect.runtime.Bar" =>
            println("it's a bar!")
            val value = messageSer[Bar].deserialize(message).content
            println(value.att1)

          case "org.scalera.reflect.runtime.Foo" =>
            val value = messageSer[Foo].deserialize(message).content
            println("it's a foo!")
            println(value.att2)

          case _ =>
            println("it's ... something!")
        }
    }
  }

  lazy val producer = new Producer {
    override def produce(message: Any): Try[Unit] = Try(consumer.consume(message))
  }

  producer.produce(messageSer[Foo].serialize(Message(foo)))

}
