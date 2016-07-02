package examples.templating

import java.net.URL

import com.twitter.finagle.http.Method.Get
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.filter.Cors.HttpFilter
import com.twitter.finagle.http.path.Root
import com.twitter.finagle.{Http, Service}
import io.fintrospect.formats.PlainText
import io.fintrospect.renderers.SiteMapModuleRenderer
import io.fintrospect.templating.{MustacheTemplates, RenderView, View}
import io.fintrospect.{ModuleSpec, RouteSpec}

object TemplatingApp extends App {

  val devMode = true
  val renderer = if (devMode) MustacheTemplates.HotReload("src/main/resources") else MustacheTemplates.CachingClasspath(".")

  val renderView = new RenderView(PlainText.ResponseBuilder, renderer)

  val module = ModuleSpec[Request, View](Root, new SiteMapModuleRenderer(new URL("http://my.cool.app")), renderView)
    .withRoute(RouteSpec().at(Get) / "echo" bindTo Service.mk { rq: Request => MustacheView(rq.uri) })

  Http.serve(":8181", new HttpFilter(Cors.UnsafePermissivePolicy).andThen(module.toService))

  println("See the Sitemap description at: http://localhost:8181")

  Thread.currentThread().join()
}
