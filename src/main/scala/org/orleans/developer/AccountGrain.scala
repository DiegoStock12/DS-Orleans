package org.orleans.developer

import org.orleans.silo.Services.Grain.Grain


class AccountGrain(_id: String) extends Grain(_id){
    def store(): Unit = {
      println("Executing store method for account grain")
    }
}
