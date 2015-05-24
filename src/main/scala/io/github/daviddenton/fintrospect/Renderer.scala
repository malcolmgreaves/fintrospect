package io.github.daviddenton.fintrospect

import com.twitter.finagle.http.path.Path

/**
 * Contract trait for the pluggable Renderers (Swagger etc..)
 */
trait Renderer[T] {
  def apply(basePath: Path, routes: Seq[Route]): T
}
