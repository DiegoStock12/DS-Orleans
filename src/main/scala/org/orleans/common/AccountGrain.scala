package org.orleans.common


class AccountGrain extends Grain{
    def store(): Unit = {
      println("Executing store method for account grain")
    }
}
