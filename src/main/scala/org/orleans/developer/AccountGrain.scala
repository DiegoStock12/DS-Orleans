package org.orleans.developer

import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Services.Grain.Grain.Receive


class AccountGrain(_id: String) extends Grain(_id) {
  override def receive: Receive = ???
}
