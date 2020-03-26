package org.orleans.silo.control

import org.orleans.silo.Services.Grain.Grain

import scala.reflect.ClassTag

/**
* Request to create a new grain
*
* @param grainClass class of the grain to create
*/
case class CreateGrainRequest(grainClass: ClassTag[_ <: Grain])

/**
 * Response to the create grain operation
 *
 * @param id id of the created grain
 * @param address address of the dispatcher for that grain
 * @param port port of the dispatcher
 */
case class CreateGrainResponse(id: String, address: String, port: Int)


/**
 * Request to find a grain
 * @param id id of the grain to be searched
 */
// TODO maybe we should allow for other ways of searching
// by overloading the constructor or optional parameters
case class SearchGrainRequest(id: String)

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

