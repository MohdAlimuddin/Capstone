package org.knoldus.database

import org.knoldus.JWT.JwtAuthorization.createToken
import org.knoldus.model.Models.{UserLogin, User}
import org.knoldus.model.UserTypes
import org.knoldus.model.UserTypes.{Admin, Customer, Moderator, PremiumCustomer, UserType}
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcType, MySQLProfile}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent._
import scala.math.Ordered.orderingToOrdered

class UserDb (db: MySQLProfile.backend.DatabaseDef)(implicit ec: ExecutionContext) extends TableQuery(new UserTable(_)){


  implicit val userTypeMapper: JdbcType[UserType] with BaseTypedType[UserType] = MappedColumnType.base[UserType, String] (
    {
      case Admin => "Admin"
      case Customer => "Customer"
      case Moderator => "Moderator"
      case PremiumCustomer => "PremiumCustomer"
    }, {
      case "Admin" => Admin
      case "Customer" => Customer
      case "Moderator" => Moderator
      case "PremiumCustomer" => PremiumCustomer
    }
  )

  def getById(id : Option[String]) : Future[Seq[User]] = {
    db.run[Seq[User]](this.filter(_.id === id).result)
  }

  def insert(user: User) : Future[Int] = {
    db.run(this += user)
  }

  def login(login : UserLogin) : Future[Option[String]] ={
    for {
      user1 <- db.run[Seq[User]](this.filter(_.username === login.username).result)
    }yield {
      if(user1.nonEmpty){
        if(user1.head.status > Some(0)){
          if(user1.head.password == login.password){
            if(user1.head.userType != UserTypes.Admin){
              if(user1.head.userType == UserTypes.PremiumCustomer) {
                updateReward(user1.head.id,20)
                Some(createToken("Premium" , 60))
              }
              else {
                updateReward(user1.head.id,10)
                Some(createToken(user1.head.userType.toString , 60))
              }
            }
            else Some(createToken("Admin" , 60))
          }
          else None
        }
        else None
      }
      else None
    }
  }

  def createModerator(id : String) : Future[Int] ={
    db.run(this.filter(_.id === id).map(_.userType).update(Moderator))
  }

  def updateReward(id : Option[String], reward : Int):Future[Boolean] = {
    val a = for{
      user <- db.run[Seq[User]](this.filter(_.id === id).result)
    }yield {
      val result = db.run(this.filter(_.id === id).map(_.reward).update(user.head.reward.flatMap(newReward => Some(reward + newReward))))
      result.flatMap{
        value =>
          if(value > 0) Future.successful(true)
          else Future.successful(false)
      }
    }
    a.flatMap(x => x)
  }

  def disableUser(id : String): Future[Int] = {
    db.run(this.filter(_.id === id).map(_.status).update(Some(0)))
  }

  def enableUser(id : String): Future[Int] = {
    db.run(this.filter(_.id === id).map(_.status).update(Some(1)))
  }

  def getAll : Future[Seq[User]] = {
    db.run(this.result)
  }

  def delete(user : User): Future[Int] ={
    db.run(this.filter(_.id === user.id).delete)
  }

  def updateUser(oldUser : User , newUser : User) : Future[Int] ={
    val result = delete(oldUser)
    val finalResult : Future[Int] = result.flatMap[Int]{
      value => {
        if (value > 0) insert(newUser)
        else Future.successful(0)
      }
    }
    finalResult
  }

  def updateUserName(user : User, name :String): Future[Int] = {
    db.run(this.filter(_.id === user.id).map(_.name).update(name))
  }

}
