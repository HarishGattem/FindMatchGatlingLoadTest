package Result

class LogResultReporter extends ResultReporter {
  override def reportMatch(busNo: String, agentNo: String, contactId: String): Unit = {
    println("Match FOUND busNo: " + busNo + " agentId: " + agentNo + " contactId: " + contactId);
  }
}
