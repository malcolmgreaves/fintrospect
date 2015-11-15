package examples.full.test.contract

import examples.full.main.{EmailAddress, Username}
import org.scalatest.{BeforeAndAfter, Ignore}

/**
 * Contract implementation for the real user directory service. Extra steps might be required here to setup/teardown
 * test data.
 */
@Ignore // this would not be ignored in reality
class RealUserDirectoryContractTest extends UserDirectoryContract with BeforeAndAfter {

  // real test data would be set up here for the required environment
  override lazy val username = Username("Elon Musk")
  override lazy val email = EmailAddress("elon@tesla.com")
  override lazy val authority = "directory.server.dns:8506"
}
