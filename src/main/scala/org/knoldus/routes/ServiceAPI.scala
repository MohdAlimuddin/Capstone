package org.knoldus.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import spray.json._
import org.knoldus.JWT.JwtAuthorization.{isTokenValidOrNot, tokenDecoder}
import org.knoldus.JWT.UserJson
import org.knoldus.database.UserDatabase
import org.knoldus.model.Models.{User, UserID, UserLogin, UserRole}
import org.knoldus.model.UserTypes
import org.knoldus.model.UserTypes._
import org.knoldus.service.UserService

import java.util.UUID.randomUUID
import scala.util._

object ServiceAPI extends App with UserJson with SprayJsonSupport {

  implicit val system = ActorSystem("System")
  implicit val materiazlizer = ActorMaterializer()


  def isTokenExpired(token: String): Boolean = tokenDecoder(token) match {
    case Success(claims) =>
      println(claims.content.parseJson.convertTo[UserRole])
      claims.expiration.getOrElse(0L) < System.currentTimeMillis() / 1000
    case Failure(_) => true
  }

  def isTokenValid(token: String): Boolean = {
    if (isTokenValidOrNot(token)) {
      tokenDecoder(token) match {
        case Success(claims) =>
          val role = claims.content.parseJson.convertTo[UserRole]
          if (UserTypes.withName(role.role) == Admin) true
          else false
        case Failure(_) => false
      }
    }
    else false
  }

  val userDB = new UserService(new UserDatabase)

  val requestHandler =
    pathPrefix("api" / "user") {
      get {
        path("users") {
          optionalHeaderValueByName("Authorization") {
            case Some(token) =>
              if (isTokenValid(token)) {
                if (isTokenExpired(token)) complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token Expired"))
                else {
                  val users = userDB.getAllUser
                  complete(users)
                }
              } else {
                complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token Invalid"))
              }
            case _ => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token not found"))
          }
        } ~
          (path(Segment) | parameter(Symbol("id"))) { id =>
            optionalHeaderValueByName("Authorization") {
              case Some(token) =>
                if (isTokenValid(token)) {
                  if (isTokenExpired(token)) {
                    complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token Expired"))
                  } else {
                    val user = userDB.getById(Some(id))
                    complete(user)
                  }
                } else {
                  complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token Invalid"))
                }
              case _ => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token not found"))
            }
          }
      } ~
        post {
          path("register") {
            entity(as[User]) {
              user =>
                val newUser = user.copy(id = Some(randomUUID().toString), reward = Some(0), status = Some(1))
                println(newUser)
                onComplete(userDB.createNewUser(newUser)) {
                  case Success(value) =>
                    if (value) complete("User Registered")
                    else complete(StatusCodes.BadRequest, "Unable to Register User")
                  case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
                }
            }
          } ~
            path("login") {
              entity(as[UserLogin]) {
                credentials =>
                  onComplete(userDB.login(credentials)) {
                    case Success(value) =>
                      value match {
                        case Some(result) =>
                          respondWithHeader(RawHeader("Access-Token", result)) {
                            complete(StatusCodes.OK, s"login Successful")
                          }
                        case None => complete(StatusCodes.BadRequest, "Invalid credentials")
                      }
                    case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
                  }
              }
            } ~
            path("create" / "moderator") {
              entity(as[UserID]) {
                userID =>
                  optionalHeaderValueByName("Authorization") {
                    case Some(token) =>
                      if (isTokenValid(token)) {
                        if (isTokenExpired(token)) {
                          complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token expired."))
                        } else {
                          onComplete(userDB.createModerator(userID.id)) {
                            case Success(value) =>
                              if (value) complete(StatusCodes.OK, s"User set as Moderator")
                              else complete(StatusCodes.InternalServerError, s"Operation Failed")
                            case Failure(exception) => complete(StatusCodes.BadRequest, s"Can't perform action because ${exception.getMessage}")
                          }
                        }
                      } else {
                        complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token Invalid"))
                      }
                    case _ => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token not found"))
                  }
              }
            } ~
            path("enable") {
              entity(as[UserID]) {
                userID =>
                  optionalHeaderValueByName("Authorization") {
                    case Some(token) =>
                      if (isTokenValid(token)) {
                        if (isTokenExpired(token)) {
                          complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token expired."))
                        } else {
                          onComplete(userDB.enableUser(userID.id)) {
                            case Success(value) =>
                              if (value) complete(StatusCodes.OK, s"User Enabled")
                              else complete(StatusCodes.InternalServerError, s"User is not enabled")
                            case Failure(exception) => complete(StatusCodes.BadRequest, s"Can't perform action because ${exception.getMessage}")
                          }
                        }
                      } else {
                        complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token Invalid"))
                      }
                    case _ => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token not found"))
                  }
              }
            } ~
            path("disable") {
              entity(as[UserID]) {
                userID =>
                  optionalHeaderValueByName("Authorization") {
                    case Some(token) =>
                      if (isTokenValid(token)) {
                        if (isTokenExpired(token)) {
                          complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token expired."))
                        } else {
                          onComplete(userDB.disableUser(userID.id)) {
                            case Success(value) =>
                              if (value) complete(StatusCodes.OK, s"User is disabled successfully")
                              else complete(StatusCodes.InternalServerError, s"User is not disabled")
                            case Failure(exception) => complete(StatusCodes.BadRequest, s"Can't perform action because ${exception.getMessage}")
                          }
                        }
                      } else {
                        complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token Invalid"))
                      }
                    case _ => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token not found"))
                  }
              }
            }
        } ~
        put {
          path("update") {
            entity(as[List[User]]) { users =>
              optionalHeaderValueByName("Authorization") {
                case Some(token) =>
                  if (isTokenValid(token)) {
                    if (isTokenExpired(token)) {
                      complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token expired."))
                    } else {
                      if (users.head.id.isEmpty || users(1).id.isEmpty) {
                        complete(StatusCodes.BadRequest)
                      }
                      else {
                        onComplete(userDB.updateUser(users.head, users(1))) {
                          case Success(value) =>
                            if (value) complete("The user is updated successfully")
                            else complete("The user is not updated successfully")
                          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
                        }
                      }
                    }
                  } else {
                    complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token Invalid"))
                  }
                case _ => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token not found"))
              }
            }
          }
        } ~
        delete {
          delete {
            path("delete"){
              entity(as[User]) { user =>
                optionalHeaderValueByName("Authorization") {
                  case Some(token) =>
                    if (isTokenValid(token)) {
                      if (isTokenExpired(token)) {
                        complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token expired."))
                      } else {
                        if(user.id.isEmpty) complete(InternalServerError, s"please provide the id of the user")
                        else{
                          onComplete(userDB.deleteUser(user)){
                            case Success(value) =>
                              if(value) complete("The user is deleted successfully")
                              else complete("The user is not deleted")
                            case Failure(ex)    => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
                          }
                        }
                      }
                    } else {
                      complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token Invalid"))
                    }
                  case _ => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token not found"))
                }

              }
            }
          }
        }
    }
  Http().bindAndHandle(requestHandler, "localhost", 8080)
}