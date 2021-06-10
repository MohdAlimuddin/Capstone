package org.knoldus.database

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.knoldus.DAO.Dao
import org.knoldus.database.UserDatabase._
import org.knoldus.model.Models.{UserLogin, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util._

class ActorModel extends Actor {

  val DBConnection = new DBConnection
  val userDb = new UserDb(DBConnection.db)

  override def receive() :Receive ={
    case CreateUser(user) =>
      val res = Future{
        Try{
          val result = userDb.insert(user)
          result.map{
            value => {
              if(value > 0) Success
              else Failure
            }
          }
        } match {
          case Failure(_) => false
          case Success(_) => true
        }
      }.recover{
        case _:RuntimeException => false
      }
      res.pipeTo(sender())

    case CreateModerator(id) =>
      val res = Future{
        Try{
          val result = userDb.createModerator(id)
          result.map{
            value =>{
              if(value > 0) Success
              else Failure
            }
          }
        } match {
          case Success(_) => true
          case Failure(_) => false
        }
      }.recover{
        case _:RuntimeException => false
      }
      res.pipeTo(sender())

    case DisableUser(id) =>
      val res = Future{
        Try{
          val result = userDb.disableUser(id)
          result.map{
            value =>{
              if(value > 0) Success
              else Failure
            }
          }
        } match {
          case Success(_) => true
          case Failure(_) => false
        }
      }.recover{
        case _:RuntimeException => false
      }
      res.pipeTo(sender())

    case EnableUser(id) =>
      val res = Future{
        Try{
          val result = userDb.enableUser(id)
          result.map{
            value =>{
              if(value > 0) Success
              else Failure
            }
          }
        } match {
          case Success(_) => true
          case Failure(_) => false
        }
      }.recover{
        case _:RuntimeException => false
      }
      res.pipeTo(sender())

    case LoginRequest(credentials) =>
      val result = {
        userDb.login(credentials)
      }.recover{
        case _:RuntimeException => None
      }
      result.onComplete{
        case Success(value) => println("Jwt Token = "+value)
      }
      result.pipeTo(sender())

    case ListAllUsers =>
      val result = {
        userDb.getAll
      }.recover{
        case _:RuntimeException => List()
      }
      val finalResult =result.map{
        seq =>
          seq.toList
      }.recover{
        case _:RuntimeException => List()
      }
      finalResult.pipeTo(sender())

    case UpdateUser(oldUser,newUser) =>
      val res =  Future{
        Try{
          val result = userDb.updateUser(oldUser,newUser)
          result.map{
            value =>{
              if(value > 0) Success
              else Failure
            }
          }
        } match {
          case Failure(_) => false
          case Success(_) => true
        }
      }.recover{
        case _:RuntimeException => false
      }
      res.pipeTo(sender())

    case UpdateUserName(user,newName) =>
      val res = Future{
        Try{
          val result = userDb.updateUserName(user,newName)
          result.map{
            value =>{
              if(value > 0) Success
              else Failure
            }
          }
        } match {
          case Success(_) => true
          case Failure(_) => false
        }
      }.recover{
        case _:RuntimeException => false
      }
      res.pipeTo(sender())

    case DeleteUser(user) =>
      val res = Future{
        Try{
          val result = userDb.delete(user)
          result.map{
            value =>{
              if(value > 0) Success
              else Failure
            }
          }
        } match {
          case Success(_) => true
          case Failure(_) =>false
        }
      }.recover{
        case _:RuntimeException => false
      }
      res.pipeTo(sender())

    case GetUserByID(id) =>
      val result = {
        userDb.getById(id)
      }.recover{
        case _:RuntimeException => List()
      }
      val secondResult =result.map{
        seq =>
          seq.toList
      }.recover{
        case _:RuntimeException => List()
      }
      val finalResult = secondResult.map{
        user => user.head
      }
      finalResult.pipeTo(sender())

  }
}

class UserDatabase extends Dao[User] {
  val system: ActorSystem = ActorSystem("System")
  val actor: ActorRef = system.actorOf(Props[ActorModel])
  implicit val timeout: Timeout = Timeout(5 seconds)

  override def createUser(user: User): Future[Boolean] =
    (actor ? CreateUser(user)).mapTo[Boolean]

  override def listAllUser(): Future[List[User]] =
    (actor ? ListAllUsers).mapTo[List[User]]

  override def updateUser(oldUser : User, newUser : User): Future[Boolean] =
    (actor ? UpdateUser(oldUser,newUser)).mapTo[Boolean]

  override def updateUserName(user: User, newName: String): Future[Boolean] =
    (actor ? UpdateUserName(user,newName)).mapTo[Boolean]

  override def deleteUser(user: User): Future[Boolean] =
    (actor ? DeleteUser(user)).mapTo[Boolean]

  override def getUserById(id: Option[String]): Future[User] =
    (actor ? GetUserByID(id)).mapTo[User]

  override def login(login: UserLogin): Future[Option[String]] =
    (actor ? LoginRequest(login)).mapTo[Option[String]]

  override def createModerator(userId: String): Future[Boolean] =
    (actor ? CreateModerator(userId)).mapTo[Boolean]

  override def disableUser(userID: String): Future[Boolean] =
    (actor ? DisableUser(userID)).mapTo[Boolean]

  override def enableUser(userID: String): Future[Boolean] =
    (actor ? EnableUser(userID)).mapTo[Boolean]
}

object UserDatabase {
  case class DisableUser(userID : String)
  case class EnableUser(userId: String)
  case class CreateModerator(userID : String)
  case class LoginRequest(login: UserLogin)
  case class CreateUser(user:User)
  case object ListAllUsers
  case class UpdateUser(user : User, newUser : User)
  case class UpdateUserName(user: User, newName: String)
  case class DeleteUser(user: User)
  case class GetUserByID(id: Option[String])
}