package org.knoldus.service

import org.knoldus.DAO.Dao
import org.knoldus.model.Models.{UserLogin, User}
import org.knoldus.model.UserTypes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserService(userDatabase: Dao[User]) {
  def createNewUser(user: User): Future[Boolean] = {
    if(user.id != null && user.name.nonEmpty && UserTypes.values.contains(user.userType)){
      userDatabase.createUser(user)
    }
    else Future(false)
  }

  def login(login: UserLogin): Future[Option[String]] ={
    userDatabase.login(login)
  }

  def createModerator(userID : String) : Future[Boolean] = {
    userDatabase.createModerator(userID)
  }

  def disableUser(userID : String) : Future[Boolean] ={
    userDatabase.disableUser(userID)
  }

  def enableUser(userID : String) : Future[Boolean] ={
    userDatabase.enableUser(userID)
  }

  def getAllUser: Future[List[User]] ={
    userDatabase.listAllUser()
  }

  def updateUser(oldUser : User, newUser : User):Future[Boolean] ={
    if(oldUser != newUser){
      userDatabase.updateUser(oldUser,newUser)
    }
    else Future(false)
  }

  def updateUserName(user : User, newName : String):Future[Boolean] ={
    if(user.name != newName && newName.nonEmpty){
      userDatabase.updateUserName(user,newName)
    }
    else {
      Future(false)
    }
  }

  def deleteUser(user : User) : Future[Boolean]={
    userDatabase.deleteUser(user)
  }

  def getById(id: Option[String]): Future[User] ={
    userDatabase.getUserById(id)
  }
}


