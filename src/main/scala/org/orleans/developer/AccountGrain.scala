package org.orleans.developer

import org.orleans.silo.Services.Grain.Grain


class AccountGrain extends Grain{
    def store(): Unit = {
      println("Executing store method for account grain")
    }
}
