package io.fintrospect.parameters

import com.twitter.finagle.http.Request

class OptionalHeaderTest extends JsonSupportingParametersTest[RequestParameter, Optional](Header.optional) {

  override def to[X](method: (String, String) => RequestParameter[X] with Optional[X], value: X): ParamBinding[X] = {
    method(paramName, null) -> value
  }

  override def attemptFrom[X](method: (String, String) => RequestParameter[X] with Optional[X], value: Option[String])= {
    val request = Request()
    value.foreach(request.headers().add(paramName, _))
    method(paramName, null).attemptToParseFrom(request)
  }
}
