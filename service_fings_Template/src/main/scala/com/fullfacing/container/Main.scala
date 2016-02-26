package com.fullfacing.template

import java.util.UUID

import akka.actor.{Actor,
 ActorSystem, Props}
import akka.routing.FromConfig
import com.fullfacing.Imports._
import com.fullfacing.common.fings.context.{MongoConnectionContext, MongoContext}
import com.fullfacing.common.fings.message._
import com.fullfacing.common.fings.model.ETemplate
import com.fullfacing.framework.AtomizerFormats.default
import com.fullfacing.framework.DefaultAtomizers._
import com.fullfacing.framework.Security._
import com.fullfacing.message.Status._
import com.fullfacing.message._
import com.fullfacing.queue.RabbitMQ._
import com.fullfacing.queue.{ActorSubscribeRequest, MessageQueue, Publish}
import com.mongodb.casbah.MongoDB
import com.rabbitmq.client.{Channel, Connection}
import org.slf4j.LoggerFactory
import com.fullfacing.message.User

import scala.language._
import scalaz.Success

/**
  * Project: TemplateService
  * Created on 24/2/2016 by Petrus de Kock <petrus@zapop.com>
  * Adapted from the MST, Ticketing Seat Planner
  */

/**
  *Edit this template with the new information for the new microservice
  * In queue.properties change the app.name to match your microservice
  * In mongo.properties change the dbname to match your microservice
  * Rename all the needed places in Project Tab
  */


object Main extends App with MongoConnectionContext {
  val logger = LoggerFactory.getLogger("TemplateLogger")
  try {
    val system = ActorSystem.create("actorsystem")

    // these are used but the ActorSubscribe to convert the incoming byte array into a concrete type that you can use
    implicit def arrayToGetAllRequest(array: Array[Byte]): Request[GetAllRequest] = array.deatomized[Request[GetAllRequest]]
    implicit def arrayToGetByIdRequest(array: Array[Byte]): Request[GetByIdRequest] = array.deatomized[Request[GetByIdRequest]]
    implicit def arrayToSaveRequest(array: Array[Byte]): Request[SaveRequest[Template]] = array.deatomized[Request[SaveRequest[Template]]]
    implicit def arrayToUpdateRequest(array: Array[Byte]): Request[UpdateRequest[Template]] = array.deatomized[Request[UpdateRequest[Template]]]

    implicit val queue = new MessageQueue[Channel, Connection]

    val templateActor = system.actorOf(Props(new TemplateActor(_db)(queue)).withRouter(FromConfig()), "TemplateActor")
    queue.subscribeRequests(
      ActorSubscribeRequest[Request[GetAllRequest]](Topics.Template.getAllRequest, templateActor),
      ActorSubscribeRequest[Request[GetByIdRequest]](Topics.Template.getByIdRequest, templateActor),
      ActorSubscribeRequest[Request[SaveRequest[Template]]](Topics.Template.saveRequest, templateActor),
      ActorSubscribeRequest[Request[UpdateRequest[Template]]](Topics.Template.updateRequest, templateActor)
    )
  } catch {
    case t: Throwable => logger.error(s"${Console.RED}Template service failed: ${t.getMessage}${Console.RESET}", t)
  }
}

class TemplateActor(val _db: MongoDB)(queue: MessageQueue[Channel, Connection]) extends Actor with MongoContext {

  import com.fullfacing.common.fings.dao.FingsMongoDAO._

  implicit val q = queue
  val logger = LoggerFactory.getLogger(classOf[TemplateActor])

  // If the mapping from entity to message changes in future make changes to these two
  //  val mapETemplateToTemplate: ETemplate => Template = ETemplate =>
  def mapETemplateToTemplate(e: ETemplate): Template =
    Template(
      /**
        * add Catagories here in order as from the Case Classes provided(Ctrl+P)
        */
    )

  def mapTemplateToETemplate(c: Template): ETemplate =
    ETemplate(
      /**
        * add Catagories here in order as from the Case Classes provided(Ctrl+P)
        */
    )

  override def receive: Receive = {
    case Request(uuid, tokenized, meta) =>
      logger.info(s"${Console.WHITE}RequestIN(${Console.RED}$uuid${Console.WHITE}):::  ${Console.MAGENTA}${tokenized.value}${Console.RESET}")
      validate(tokenized) match {

        case Success(GetAllRequest(user: Option[User])) =>
          GetAllResponse(template.list().map(mapETemplateToTemplate).toList)
            .toResponse(OK, "OK")(uuid).publish(Topics.Template.getAllResponse)

        case Success(GetByIdRequest(ids, user: Option[User])) =>
          val eTemplateList = ids.flatMap(id =>
            template.headOption(ETemplate.id === Some(id)).map(mapETemplateToTemplate).toList)
          GetByIdResponse(eTemplateList)
            .toResponse(OK, "OK")(uuid).publish(Topics.Template.getByIdResponse)

        case Success(SaveRequest(templateToSave: Template, user: Option[User])) =>
          (template.headOption(ETemplate.templateUUID === templateToSave.templateUUID) match {
            case Some(existingTemplate) =>
              SaveResponse(None).toResponse(BAD_REQUEST, "Template exists, use UpdateRequest instead")
            case None =>
              val savedTemplate = template += mapTemplateToETemplate(templateToSave)
              SaveResponse[Template](Some(templateToSave.copy(id = savedTemplate.id))).toResponse(CREATED, "Template Saved")
          }) (uuid).publish(Topics.Template.saveResponse)

        case Success(UpdateRequest(templateToUpdate: Template, user: Option[User])) =>
          template.headOption(ETemplate.templateUUID === templateToUpdate.templateUUID).map { _ =>
            template := mapTemplateToETemplate(templateToUpdate)
            UpdateResponse(Some(templateToUpdate)).toResponse(ACCEPTED, "Template Updated")
          }.getOrElse(UpdateResponse(None).toResponse(BAD_REQUEST, "Template does not exist, use SaveRequest instead"))(uuid).publish(Topics.Template.updateResponse)

        case x =>
          println("Error on recieved message: " + x)
          Publish("NO TOPIC EVER", "Should never get HERE something is afoot".atomized)
      }

    case x =>
      println("something went wrong: " + x) // got data type not looking for
  }

}
