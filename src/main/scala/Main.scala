import scala.concurrent.duration._

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import SessionManager._

object SessionManager {
  sealed trait Command
  case class Login(session: String, replyTo: ActorRef[LoginAttempt]) extends Command

  case class LoginAttempt(descriptor: String)

  def apply(): Behavior[Command] = Behaviors.setup(new SessionManager(_))
}

class SessionManager(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {

  override def onMessage(msg: Command): Behavior[Command] = Behaviors.receiveMessage {
    case Login(session, replyTo) =>
      replyTo ! LoginAttempt(s"Hi $session!")
      Behaviors.same
  }
}

object Main extends App {
  val system: ActorSystem[Command] = 
    ActorSystem(SessionManager(), "reproducer")

  // This is printed:
  system.log.info("Started")

  val sessionManager: ActorRef[Command] = system

  implicit val timeout = Timeout(1.second)
  implicit val scheduler = system.scheduler
  implicit val ec = system.toClassic.dispatcher

  sessionManager.ask[LoginAttempt](Login("Foo", _)).foreach { response =>
    // This is not printed:
    system.log.info(s"Got response $response")
  }
  sessionManager.ask[LoginAttempt](Login("Bar", _)).foreach { response =>
    // This is printed:
    system.log.info(s"Got response $response")
  }
}
