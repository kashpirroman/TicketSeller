package TicketSeller
import TicketSeller.Operations.AccessLevel.AccessLevel
import TicketSeller.Operations.EventOperations._
import TicketSeller.Operations.{Role, Unauthorized}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

import scala.concurrent.{ExecutionContext, Future}

class RestApi(system: ActorSystem, timeout: Timeout) extends BoxOfficeApi with JsonCodec with UriDecoder {
  override implicit def executionContext: ExecutionContext = system.dispatcher
  override implicit def requestTimeout: Timeout = timeout
  override def createBoxOffice(): ActorRef =system.actorOf(BoxOffice.property,BoxOffice.name)

  def eventsRoute=pathPrefix("events") {
    pathEndOrSingleSlash {
      get {
        onSuccess(getEvents(Unauthorized)) {
          case GetEventsResponse(eventList,user) => complete(eventList)
          case CancelEventResponse(message,user) => complete(message)
        }
      }
    }
  }
  def eventRoute=pathPrefix("events"/Segment){
    event=>
      pathEndOrSingleSlash{
        get{
          onSuccess(getEvent(fromUriToEvent(event),Unauthorized)) {
            case GetEventResponse(event,user) => complete(event)
            case CancelEventResponse(message,user) => complete(message)
          }
        }
      }
    }



  def routes:Route=eventsRoute~eventRoute
}

trait BoxOfficeApi{
  implicit def executionContext: ExecutionContext

  implicit def requestTimeout: Timeout
  lazy val boxOffice: ActorRef = createBoxOffice()

  def createBoxOffice():ActorRef

  def getEvents[T <: AccessLevel](user:Role[T]): Future[EventResponse[T]] =
    boxOffice.ask(GetEvents(user)).mapTo[EventResponse[T]]

  def getEvent[T <: AccessLevel](event: Either[String,Event],user:Role[T]): Future[EventResponse[T]] = {
    event match {
      case Right(value) => boxOffice.ask(GetEvent(value,user)).mapTo[EventResponse[T]]
      case Left(value) =>  Future{CancelEventResponse(value,user)}
    }
   }


}

