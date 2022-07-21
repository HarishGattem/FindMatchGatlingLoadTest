package Configuration

import scala.util.Properties

class Configuration {
  val contacts = Properties.propOrElse("contacts_per_skill", "1")
  val numOfSkills = Properties.propOrElse("skills", "1")
  val numOfAgents = Properties.propOrElse("agents", "1")
  val clusterName = Properties.propOrElse("cluster_name", "TEST3")
  val streamName = Properties.propOrElse("stream_name", "dev-findmatch-matchrequests")
  val queueName = Properties.propOrElse("queue_name", "TEST3-Matches")
}
