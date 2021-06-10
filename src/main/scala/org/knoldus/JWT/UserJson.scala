package org.knoldus.JWT

import org.knoldus.model.Models._
import org.knoldus.model.UserTypes
import spray.json._

trait UserJson extends DefaultJsonProtocol {

  implicit object UserTypeJsonFormat extends JsonFormat[UserTypes.Value] {
    def write(obj: UserTypes.Value): JsValue = JsString(obj.toString)
    def read(json: JsValue): UserTypes.Value = json match {
      case JsString(txt) => UserTypes.withName(txt)
      case _             => throw DeserializationException(s"Expected a value from enum $UserTypes")
    }
  }


  implicit val userFormat: RootJsonFormat[User] = jsonFormat7(User)
  implicit val loginFormat : RootJsonFormat[UserLogin] = jsonFormat2(UserLogin)
  implicit val userRoleFormat: RootJsonFormat[UserRole] = jsonFormat1(UserRole)
  implicit val userIdFormat : RootJsonFormat[UserID] = jsonFormat1(UserID)
}
