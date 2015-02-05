import akka.actor._
import akka.persistence._
import scala.concurrent.duration._

case object CreateSnapshot
case object CreatePieceOState
case class NewStateEvt(state: (Array[Byte], Deadline))

class LeakyJournalActor extends PersistentActor with ActorLogging {
    import context._
    
    log.info(s"Starting")
    val createSnapshotTimer = context.system.scheduler.schedule(5.seconds, 60.seconds, self, CreateSnapshot)
    //I've seen a situation where the time it takes to do a snapshot increases over time. As a result, 
    //the snapshot request can clump together. When a large number of snapshot requests happen at the 
    //same time, it can blow my heap space limit. This is my workaround.
    protected var snapshotInProgress = false 
    
    val createStateTimer = context.system.scheduler.schedule(5.seconds, 10.seconds, self, CreatePieceOState)
    
    override def persistenceId = "LeakyJournalActor"
    
    protected var importantState = collection.mutable.LinkedList.empty[(Array[Byte], Deadline)]
    
    override def preRestart(reason: Throwable, message: Option[Any]) {
        createSnapshotTimer.cancel
        createStateTimer.cancel
    }
    
    def receiveRecover: Receive = {
        case SnapshotOffer(metadata, offeredSnapshot) =>
            log.info(s"Using snapshot ${metadata.persistenceId} ${metadata.sequenceNr} ${metadata.timestamp}")
            importantState = offeredSnapshot.asInstanceOf[collection.mutable.LinkedList[(Array[Byte], Deadline)]]
        case n : NewStateEvt =>
            importantState = n.state +: importantState
            log.info(s"NewStateEvt being replayed. There are ${importantState.size} pieces of state.")
        case RecoveryCompleted =>
            log.info(s"Recovery Complete. There are ${importantState.size} pieces of state.")
    }

    def receiveCommand: Receive = {
        case CreateSnapshot =>
    
            //In real code, we'd have another actor consume these pieces of state 
            //and then we'd get rid of them once the processing was complete.
            //In this example, I'll just have them time out because it's easier.
            val before = importantState.size
            importantState = importantState filter {x => !x._2.isOverdue}
            val after = importantState.size
            if (before != after){
                log.info(s"${before-after} pieces of state expired. ${after} remain.")
            }
    
            snapshotInProgress match {
              case true =>
                  log.info(s"Snapshot already in progress. Will not start a new one.")
              case _ =>
                  log.info(s"Saving snapshot. ${importantState.size} pieces of state in importantState.")
                  saveSnapshot(importantState)
                  snapshotInProgress = true
            }
        case SaveSnapshotSuccess(metadata) => 
            snapshotInProgress = false
            log.info(s"Snapshot saved.")
            log.info(s"Deleting messages and snapshots from before: ${metadata.sequenceNr}")
            log.info(s"Deleting snapshots from before: ${metadata.timestamp}")
            //I believe I am calling these in such a way that should ensure that files don't accumulate in 
            //the journal and snapshot directories.
            deleteMessages(metadata.sequenceNr, true)
            deleteSnapshots(SnapshotSelectionCriteria(maxSequenceNr = metadata.sequenceNr, maxTimestamp = metadata.timestamp-1))
        case SaveSnapshotFailure(metadata, reason) =>
            snapshotInProgress = false
            log.warning(s"Failed to save snapshot. Reason: ${reason}, ${reason.getStackTrace().map(f => f.toString()).mkString("\n")}, ${reason.getCause()}, ${reason.getMessage()}")
        case CreatePieceOState =>
            val bytes = Array.fill(12*1024*1024)((scala.util.Random.nextInt(256) - 128).toByte)
            val deadline = 3.minutes.fromNow
            persist(new NewStateEvt((bytes, deadline))){x =>
                importantState = (bytes, deadline) +: importantState
                log.info(s"Added another piece of state. ${importantState.size} pieces of state in importantState.")
            }
    }
}

object LeakyJournalIssue extends App {
    val system = ActorSystem("journal-issue")
    val leaky = system.actorOf(Props[LeakyJournalActor], "leaky")
}