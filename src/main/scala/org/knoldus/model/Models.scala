package org.knoldus.model

import org.knoldus.model.UserTypes.UserType

object Models {
  case class User(id:Option[String], name:String, username:String, password:String, userType: UserType, reward:Option[Int], status:Option[Int])

  case class UserLogin(username:String, password:String)

  case class UserID(id: String)

  case class UserRole(role:String)
}