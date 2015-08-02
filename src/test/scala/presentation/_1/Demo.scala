package presentation._1

import com.twitter.finagle.Http
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.path.Root
import io.fintrospect.renderers.simplejson.SimpleJson
import io.fintrospect.{CorsFilter, FintrospectModule}

class SearchApp {
  val service = FintrospectModule(Root, SimpleJson()).toService
  val searchService = new CorsFilter(Cors.UnsafePermissivePolicy).andThen(service)
  Http.serve(":9000", searchService)
}


object Environment extends App {
  new SearchApp
  Thread.currentThread().join()
}

/**
 * showcase: empty api and 404
 */