package presentation._3

import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.filter.Cors.HttpFilter
import com.twitter.finagle.http.path.Root
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service}
import com.twitter.util.Future
import io.fintrospect.formats.PlainText.ResponseBuilder._
import io.fintrospect.formats.ResponseBuilder._
import io.fintrospect.parameters.Query
import io.fintrospect.renderers.swagger2dot0.{ApiInfo, Swagger2dot0Json}
import io.fintrospect.{ModuleSpec, RouteSpec}
import presentation.Books

class SearchRoute(books: Books) {
  private val titlePartParam = Query.required.string("titlePart")

  def search() = new Service[Request, Response] {
    override def apply(request: Request): Future[Response] = {
      val titlePart = titlePartParam <-- request
      val results = books.titles().filter(_.toLowerCase.contains(titlePart.toLowerCase))
      Ok(results.toString())
    }
  }

  val route = RouteSpec("search books")
    .taking(titlePartParam)
    .at(Get) / "search" bindTo search
}


class SearchApp(books: Books) {
  private val apiInfo = ApiInfo("search some books", "1.0", Option("an api for searching our book collection"))

  val service = ModuleSpec(Root, Swagger2dot0Json(apiInfo))
    .withRoute(new SearchRoute(books).route)
    .toService

  val searchService = new HttpFilter(Cors.UnsafePermissivePolicy).andThen(service)
  Http.serve(":9000", searchService)
}


object Environment extends App {
  new SearchApp(new Books)
  Thread.currentThread().join()
}

/**
 * showcase: missing query param and successful search
 */
