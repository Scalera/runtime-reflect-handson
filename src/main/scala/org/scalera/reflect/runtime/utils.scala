package org.scalera.reflect.runtime

import spray.json.JsonFormat

object utils {

  def jsonSer[T](jf: JsonFormat[T]): JsonSer[T] =
    new JsonSer[T] {
      def serialize(t: T): String = jf.write(t).prettyPrint
      def deserialize(json: String): T = jf.read(spray.json.JsonParser(json))
    }

}
