package org.knoldus.DAO

import org.knoldus.model.Models.{UserLogin, User}
import scala.concurrent.Future

trait Dao[T] {
  def createUser(obj : T) : Future[Boolean]

  def login(login: UserLogin) : Future[Option[String]]

  def createModerator(userId : String) : Future[Boolean]

  def disableUser(userID : String) : Future[Boolean]

  def enableUser(userID : String) : Future[Boolean]

  def listAllUser():Future[List[T]]

  def updateUser(oldObject: T, newObject:T): Future[Boolean]

  def deleteUser(obj : T) : Future[Boolean]

  def updateUserName(obj : T , newName : String) : Future[Boolean]

  def getUserById(id: Option[String]) :Future[User]
}
