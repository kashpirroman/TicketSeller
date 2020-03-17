package TicketSeller

import TicketSeller.Operations.EventOperations.{Event, EventDateTime, EventInfo, Ticket}
import TicketSeller.Operations.Place
import org.joda.time.LocalDateTime
import scalikejdbc.WrappedResultSet
import cats.implicits._
import scalikejdbc.jodatime.JodaTypeBinder._

trait DatabaseRowCoder {

  implicit class WrappedResultSetOpt( result: WrappedResultSet) {
    def toTicket: Ticket = {
      Ticket(result.string("EventName"),
        result.int("TicketId"),
        result.string("TicketType"))
    }

    def toEventWithoutInfo: () => Event = ()=>
      Event(None,
        result.string("EventName"),
        None,
        None,
        EventDateTime(result.get[LocalDateTime]("DATETIME")))
    def toPlaceWithoutId: () => Option[Place] = ()=>Option(
      Place( name=result.string("Name"),
        address=result.string("Address")))
    def toEventWithPlace: () => Event = toEventWithoutInfo.map(_.copy(place = toPlaceWithoutId()))
    def toEventInfo: () => Option[EventInfo] = ()=>Option(EventInfo(result.stringOpt("Preview")))
    def toUserEventInfo: () => Event = toEventWithPlace.map(_.copy(eventInfo = toEventInfo ()))
  }
}