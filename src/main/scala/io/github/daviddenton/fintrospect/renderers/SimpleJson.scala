package io.github.daviddenton.fintrospect.renderers

import argo.jdom.JsonNodeFactories.string
import argo.jdom.JsonRootNode
import io.github.daviddenton.fintrospect.util.ArgoUtil._
import io.github.daviddenton.fintrospect.{ModuleRoute, Renderer}

class SimpleJson private() extends Renderer {
  private def render(mr: ModuleRoute): Field = {
    mr.completedPath.method + ":" + mr.toString -> string(mr.description.name)
  }

  def apply(mr: Seq[ModuleRoute]): JsonRootNode = obj("resources" -> obj(mr.map(render)))
}

/**
 * Ultra-basic Renderer implementation that only supports the route paths and the main descriptions of each.
 */
object SimpleJson {
  def apply(): Renderer = new SimpleJson()
}