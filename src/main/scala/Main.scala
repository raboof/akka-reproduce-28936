import scala.concurrent.duration._

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

object SessionManager {
  sealed trait Command
  case class Login(session: String, replyTo: ActorRef[LoginAttempt]) extends Command

  case class LoginAttempt(descriptor: String)

  def apply(): Behavior[Command] = Behaviors.receiveMessage {
    case Login(session, replyTo) =>
      replyTo ! LoginAttempt(s"Hi $session!")
      Behaviors.same
  }
}

object EngineExtension extends ExtensionId[EngineExtension] {

  override def createExtension(system: ActorSystem[_]): EngineExtension = new EngineExtension(system)
}

class EngineExtension(system: ActorSystem[_]) extends Extension {
  val sessionManager: ActorRef[SessionManager.Command] = {
    val ref = system.toClassic.spawn(SessionManager(), name = "session-manager")
    //ref ! Ping // Remove this and the initial ask always fails
    ref
  }
}

object Main extends App {
  val system = akka.actor.ActorSystem("reproducer")

  implicit val timeout = Timeout(1.second)
  implicit val scheduler = system.toTyped.scheduler
  implicit val ec = system.dispatcher

    val engineExtension = EngineExtension(system.toTyped)

    import SessionManager._
    val session = "Foo"
    engineExtension.sessionManager.ask[LoginAttempt](Login(session, _)).foreach { response =>
      system.log.info(s"Got response $response")
      system.terminate()
    }
}
