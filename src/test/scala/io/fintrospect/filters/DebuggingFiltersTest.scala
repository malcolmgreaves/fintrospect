package io.fintrospect.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.{Await, Future}
import org.scalatest.{FunSpec, ShouldMatchers}

class DebuggingFiltersTest extends FunSpec with ShouldMatchers {

  describe("Debugging") {
    it("PrintRequestAndResponse") {
      Await.result(DebuggingFilters.PrintRequestAndResponse.andThen((_: Request) => Future.value(Response()))(Request()))
    }
  }

}
