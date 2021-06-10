package org.knoldus.model

object UserTypes extends Enumeration {
  type UserType = Value
  val Customer, PremiumCustomer, Admin, Moderator = Value
}
