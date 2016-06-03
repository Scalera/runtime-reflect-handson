package org.scalera.reflect.runtime

import scala.util.Try

//  End points

trait Producer{
  def produce(message: Any): Try[Unit]
}

trait Consumer{
  val consume: Any => Unit
}

//  Messages

case class Foo(att1: Int, att2: String)

case class Bar(att1: String)


//  Serialization

trait JsonSer[T] {
  def serialize(t: T): String
  def deserialize(json: String): T
}