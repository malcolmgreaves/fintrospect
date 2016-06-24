package io.fintrospect.parameters

import com.twitter.finagle.http.Request
import org.jboss.netty.handler.codec.http.QueryStringDecoder

import scala.collection.JavaConverters._

trait QueryParameter[T]
  extends Parameter with Bindable[T, QueryBinding] {
  override val where = "query"
}

private object QueryExtractAndRebind extends ParameterExtractAndBind[Request, QueryBinding] {
  def newBinding(parameter: Parameter, value: String) = new QueryBinding(parameter, value)

  def valuesFrom(parameter: Parameter, request: Request): Option[Seq[String]] =
    Option(new QueryStringDecoder(request.uri).getParameters.get(parameter.name)).map(_.asScala.toSeq)
}

abstract class SingleQueryParameter[T](spec: ParameterSpec[T])
  extends SingleParameter(spec, QueryExtractAndRebind) with QueryParameter[T] {
}

abstract class MultiQueryParameter[T](spec: ParameterSpec[T])
  extends MultiParameter(spec, QueryExtractAndRebind) with QueryParameter[Seq[T]] {
}