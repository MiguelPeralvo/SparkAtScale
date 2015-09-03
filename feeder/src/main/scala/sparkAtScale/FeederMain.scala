package sparkAtScale

import akka.actor.ActorSystem

object FeederMain extends App {

  val system = ActorSystem("MyActorSystem")
  val feederActor = system.actorOf(FeederActor.props, "feederActor")

  system.awaitTermination()

}
