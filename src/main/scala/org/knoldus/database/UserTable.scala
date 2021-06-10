package org.knoldus.database

import org.knoldus.model.Models.User
import org.knoldus.model.UserTypes._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape


class UserTable(tag: Tag) extends Table[User] (tag, "USERS"){

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

  def id : Rep[Option[String]] = column[Option[String]]("USER_ID")
  def name: Rep[String] = column[String]("USER_NAME")
  def username : Rep[String] = column[String]("USER_USERNAME")
  def password : Rep[String] = column[String]("USER_PASSWORD")
  def userType : Rep[UserType] = column[UserType]("USER_USERTYPE")
  def reward   : Rep[Option[Int]] = column[Option[Int]]("REWARD")
  def status : Rep[Option[Int]] = column[Option[Int]]("STATUS")

  def * : ProvenShape[User] = (id,name,username,password,userType,reward,status) <> (User.tupled, User.unapply)
}
