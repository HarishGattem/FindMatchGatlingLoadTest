package Result

trait ResultReporter {
  def reportMatch(busNo: String, agentNo: String, contactId: String)
}
