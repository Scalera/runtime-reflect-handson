package org.scalera.reflect.runtime

import spray.json.DefaultJsonProtocol._

import scala.util.Try
import scala.reflect.runtime.{universe => ru}

object GenSer {

  import scala.tools.reflect.ToolBox

  //  Scala compiler tool box
  private val tb = ru.runtimeMirror(this.getClass.getClassLoader).mkToolBox()

  def genericDeserialize(msg: String)(serContainers: Seq[AnyRef]): Any = {

    val messageType = Message.typeFrom(msg)

    val serContainersImport = serContainers.map(container =>
      "import " + container.toString.split("\\$").head + "._").mkString(";\n")

    val expr =
      s"""
         |{
         |  import scala.reflect._;
         |  import spray.json._;
         |  import org.scalera.reflect.runtime._;
         |  $serContainersImport;
         |
         |  implicitly[JsonSer[Message[$messageType]]]
         |}
        """.stripMargin

    tb.eval(tb.parse(expr)).asInstanceOf[JsonSer[Message[Any]]].deserialize(msg).content
  }

}

object case3 extends App {

  import utils._
  import Message._
  import GenSer._

  implicit val fooSerializer: JsonSer[Foo] =
    jsonSer(jsonFormat2(Foo.apply))

  implicit val barSerializer: JsonSer[Bar] =
    jsonSer(jsonFormat1(Bar.apply))

  val foo: Foo = Foo(1, "hi")

  lazy val consumer = new Consumer {
    override val consume: Any => Unit = { case msg: String =>
      genericDeserialize(msg)(Seq(case3,Message)) match {
        case bar: Bar => println("it's a bar!")
        case foo: Foo => println("it's a foo!")
        case _ => println("it's ... something!")
      }
    }
  }

  lazy val producer = new Producer {
    override def produce(message: Any): Try[Unit] = Try(consumer.consume(message))
  }

  producer.produce(messageSer[Foo].serialize(Message(foo)))

}
