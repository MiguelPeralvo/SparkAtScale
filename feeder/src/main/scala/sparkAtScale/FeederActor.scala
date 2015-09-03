package sparkAtScale 

import akka.actor.{Props, Actor, ActorLogging}
import org.apache.kafka.clients.producer.{ProducerRecord,Callback,RecordMetadata}

import scala.concurrent.duration.Duration
import scala.concurrent.duration._

/**
 * This keeps the file handle open and just reads on line at fixed time ticks.
 * Not the most efficient implementation, but it is the easiest.
 */
class FeederActor extends Actor with ActorLogging with FeederExtensionActor {

  import FeederActor.SendNextLine

  var counter = 0

  implicit val executionContext = context.system.dispatcher

  val feederTick = context.system.scheduler.schedule(Duration.Zero, 100.millis, self, SendNextLine)

  var dataIter:Iterator[String] = initData()

  def receive = {
    case SendNextLine if dataIter.hasNext =>
      val nxtRating = dataIter.next()
      log.info(s"Sending next rating: $nxtRating")
      //rating data has the format user_id:movie_id:rating:timestamp
      //the key for the producer record is user_id + movie_id
      val ratingData: Array[String] = nxtRating.split(":")
      val key = ratingData(0) + ratingData(1)
      val record = new ProducerRecord[String,String](feederExtension.kafkaTopic, key, nxtRating)
      val future = feederExtension.producer.send(record, new Callback {
        override def onCompletion(result: RecordMetadata, exception: Exception) {
          if (exception != null) log.info("Failed to send record: " + exception)
        }
      })
      // Use future.get() to make this a synchronous write
  }

  def initData() = {
    val source = scala.io.Source.fromFile(feederExtension.file)
    source.getLines()
  }
}

object FeederActor {
	val props = Props[FeederActor]
	case object ShutDown
	case object SendNextLine

}
