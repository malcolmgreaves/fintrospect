package io.fintrospect

import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.path.Path
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.{Filter, Service, SimpleFilter}
import com.twitter.util.Future
import io.fintrospect.FintrospectModule._
import io.fintrospect.Headers._
import io.fintrospect.Routing.fromBinding
import io.fintrospect.parameters.{NoSecurity, Security}
import io.fintrospect.renderers.ModuleRenderer

import scala.PartialFunction._

object FintrospectModule {

  private type Binding = PartialFunction[(Method, Path), Service[Request, Response]]

  private type TFilter = Filter[Request, Response, Request, Response]

  /**
    * Combines many modules
    */
  def combine(modules: FintrospectModule*): Binding = modules.map(_.totalBinding).reduce(_.orElse(_))

  /**
    * Convert a Binding to a Finagle Service
    */
  def toService(binding: Binding): Service[Request, Response] = fromBinding(binding)

  /**
    * Create a module using the given base-path, renderer.
    */
  def apply(basePath: Path, moduleRenderer: ModuleRenderer): FintrospectModule =
    new FintrospectModule(basePath, moduleRenderer, identity, Nil, NoSecurity, Filter.mk((in, svc) => svc(in)))

  /**
    * Create a module using the given base-path, renderer and module filter (to be applied to all matching requests to this module).
    */
  def apply(basePath: Path, moduleRenderer: ModuleRenderer, moduleFilter: Filter[Request, Response, Request, Response]): FintrospectModule = {
    new FintrospectModule(basePath, moduleRenderer, identity, Nil, NoSecurity, moduleFilter)
  }

  private class ValidateParams(serverRoute: ServerRoute, moduleRenderer: ModuleRenderer) extends SimpleFilter[Request, Response]() {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      val missingOrFailed = serverRoute.missingOrFailedFrom(request)
      if (missingOrFailed.isEmpty) service(request) else Future.value(moduleRenderer.badRequest(missingOrFailed))
    }
  }

  private class Identify(route: ServerRoute, basePath: Path) extends SimpleFilter[Request, Response]() {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      val url = if (route.describeFor(basePath).length == 0) "/" else route.describeFor(basePath)
      request.headerMap.set(IDENTIFY_SVC_HEADER, request.method + ":" + url)
      service(request)
    }
  }

}

/**
  * Self-describing module builder (uses the immutable builder pattern).
  */
class FintrospectModule private(basePath: Path,
                                moduleRenderer: ModuleRenderer,
                                descriptionRoutePath: Path => Path,
                                routes: Seq[ServerRoute],
                                security: Security,
                                moduleFilter: Filter[Request, Response, Request, Response]) {
  private def totalBinding = {
    withDefault(routes.foldLeft(empty[(Method, Path), Service[Request, Response]]) {
      (currentBinding, route) =>
        val filters = new Identify(route, basePath) +: new ValidateParams(route, moduleRenderer) +: moduleFilter +: Seq[TFilter]()
        currentBinding.orElse(route.toPf(basePath)(filters.reduce(_.andThen(_))))
    })
  }

  /**
    * Set the API security for this module. Any parameters from the Security will be added to all routes.
    */
  def securedBy(newSecurity: Security): FintrospectModule = {
    new FintrospectModule(basePath, moduleRenderer, descriptionRoutePath, routes, newSecurity, moduleFilter)
  }

  /**
    * Override the path from the root of this module (incoming) where the default module description will live.
    */
  def withDescriptionPath(newDefaultRoutePath: Path => Path): FintrospectModule = {
    new FintrospectModule(basePath, moduleRenderer, newDefaultRoutePath, routes, security, moduleFilter)
  }

  private def withDefault(otherRoutes: PartialFunction[(Method, Path), Service[Request, Response]]) = {
    val descriptionRoute = new IncompletePath0(RouteSpec("Description route"), Get, descriptionRoutePath).bindTo(() => new Service[Request, Response] {
      override def apply(request: Request) = Future.value(moduleRenderer.description(basePath, security, routes))
    })

    otherRoutes.orElse(descriptionRoute.toPf(basePath)(new Identify(descriptionRoute, basePath)))
  }

  /**
    * Attach described Route(s) to the module. Request matching is attempted in the same order as in which this method is called.
    */
  def withRoute(newRoutes: ServerRoute*): FintrospectModule = new FintrospectModule(basePath, moduleRenderer, descriptionRoutePath, routes ++ newRoutes, security, moduleFilter)

  /**
    * Attach described Route(s) to the module. Request matching is attempted in the same order as in which this method is called.
    */
  def withRoutes(newRoutes: Iterable[ServerRoute]*): FintrospectModule = newRoutes.flatten.foldLeft(this)(_.withRoute(_))

  /**
    * Finaliser for the module builder to combine itself with another module into a Partial Function which matches incoming requests.
    */
  def combine(that: FintrospectModule): Binding = totalBinding.orElse(that.totalBinding)

  /**
    * Finaliser for the module builder to convert itself to a Finagle Service. Use this function when there is only one module.
    */
  def toService: Service[Request, Response] = FintrospectModule.toService(totalBinding)
}
