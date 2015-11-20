package examples.full.test.feature

import com.twitter.finagle.http.Method.Post
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status._
import examples.full.main._
import examples.full.test.env.RunningTestEnvironment
import org.scalatest.{FunSpec, ShouldMatchers}

class AttemptToGainEntryTest extends FunSpec with ShouldMatchers with RunningTestEnvironment {

  describe("for an unknown user") {
    it("does not allow user in") {
      val request = Request(Post, "/security/knock?username=Rita")
      request.headerMap("key") = "realSecret"
      val (status, _) = env.responseTo(request)
      status shouldBe NotFound
    }
  }

  describe("rejects when key is incorrect") {
    it("rejects the entry attempt") {
      val request = Request(Post, "/security/knock?username=Bob")
      request.headerMap("key") = "fakeSecret"
      val (status, _) = env.responseTo(request)
      status shouldBe Unauthorized
    }

    it("logs nothing") {
      env.entryLogger.entries shouldBe Nil
    }
  }

  describe("does allow known user in") {
    env.userDirectory.contains(User(Id(1), Username("Bob"), EmailAddress("bob@bob.com")))

    it("does not allow user to exit when not inside") {
      val request = Request(Post, "/security/bye?username=Bob")
      request.headerMap("key") = "realSecret"
      val (status, _) = env.responseTo(request)
      status shouldBe BadRequest
    }

    it("allows the user in") {
      val request = Request(Post, "/security/knock?username=Bob")
      request.headerMap("key") = "realSecret"
      val (status, _) = env.responseTo(request)
      status shouldBe Ok
    }

    it("logs entry") {
      env.entryLogger.entries shouldBe Seq(UserEntry("Bob", goingIn = true, env.clock.millis()))
    }

    it("does not allow user to enter once inside") {
      val request = Request(Post, "/security/knock?username=Bob")
      request.headerMap("key") = "realSecret"
      val (status, _) = env.responseTo(request)
      status shouldBe BadRequest
    }

    it("allows the user to exit") {
      val request = Request(Post, "/security/bye?username=Bob")
      request.headerMap("key") = "realSecret"
      val (status, _) = env.responseTo(request)
      status shouldBe Ok
    }

    it("logs exit") {
      env.entryLogger.entries shouldBe Seq(
        UserEntry("Bob", goingIn = true, env.clock.millis()),
        UserEntry("Bob", goingIn = false, env.clock.millis())
      )
    }
  }
}