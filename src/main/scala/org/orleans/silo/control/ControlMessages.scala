package org.orleans.silo.control

import org.orleans.silo.Services.Grain.Grain

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
* Request to create a new grain
*
* @param grainClass class of the grain to create
*/
case class CreateGrainRequest[T <: Grain](grainClass: ClassTag[T], grainType: TypeTag[T])

/**
 * Response to the create grain operation
 *
 * @param id id of the created grain
 * @param address address of the dispatcher for that grain
 * @param port port of the dispatcher
 */
case class CreateGrainResponse(id: String, address: String, port: Int)

/**
 * Request to activate existing grain
 *
 * @param grainClass class of the grain to create
 */
case class ActivateGrainRequest(id: String, grainClass: ClassTag[_ <: Grain])

/**
 * Response to the create grain operation
 *
 * @param id id of the activated grain
 * @param address address of the dispatcher for that grain
 * @param port port of the dispatcher
 */
case class ActivateGrainResponse(id: String, address: String, port: Int)


case class ActiveGrainRequest(id: String, grainType: TypeTag[_ <: Grain])

/**
 * Request to find a grain
 * @param id id of the grain to be searched
 */
// TODO maybe we should allow for other ways of searching
// by overloading the constructor or optional parameters
case class SearchGrainRequest(id: String, grainClass: ClassTag[_ <: Grain])

/**
 * Response to a grain search
 * @param address address of that grain's dispatcher
 * @param port port of that grain's dispatcher
 */
case class SearchGrainResponse(address: String, port : Int)


/**
 * Request to delete the grain
 *
 * @param id id of the grain to be deleted
 */
case class DeleteGrainRequest(id: String)




