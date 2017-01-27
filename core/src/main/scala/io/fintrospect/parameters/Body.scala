package io.fintrospect.parameters

import com.twitter.finagle.http.Message
import com.twitter.io.Buf
import io.fintrospect.ContentType
import io.fintrospect.formats.{Argo, JsonLibrary}
import io.fintrospect.util.Extractor

import scala.xml.Elem

trait Body[T] extends Iterable[BodyParameter]
  with Mandatory[Message, T]
  with Rebindable[Message, T, RequestBinding] {

  override def <->(from: Message): Iterable[RequestBinding] = this --> (this <-- from)

  val contentType: ContentType
}

/**
  * Factory methods for various supported HTTP body types.
  */
object Body {

  /**
    * Create a custom body type for the request. Encapsulates the means to insert/retrieve into the request
    */
  def apply[T](bodySpec: BodySpec[T], description: String = null): UniBody[T] = new UniBody[T](description, bodySpec, None)

  /**
    * Create a custom body type for the request. Encapsulates the means to insert/retrieve into the request
    */
  def apply[T](bodySpec: BodySpec[T], description: String, example: T): UniBody[T] = new UniBody[T](description, bodySpec, Option(example))

  /**
    * String HTTP message body. Defaults to empty is invalid, but this can be overridden.
    */
  def string[T](contentType: ContentType, description: String = null, validation: StringValidations.Rule = StringValidations.EmptyIsInvalid): UniBody[String] = Body(BodySpec.string(contentType, validation), description)

  /**
    * JSON format HTTP message body. Defaults to Argo JSON format, but this can be overridden by passing an alternative JsonFormat
    */
  def json[T](description: String = null, example: T = null, jsonLib: JsonLibrary[T, _] = Argo): UniBody[T] = Body(BodySpec.json(jsonLib), description, example)

  /**
    * Binary HTTP body, with custom ContentType
    */
  def binary(contentType: ContentType, description: String = null): UniBody[Buf] = Body(BodySpec.binary(contentType), description, null)

  /**
    * Native Scala XML format HTTP message body.
    */
  def xml(description: String = null, example: Elem = null): UniBody[Elem] = Body(BodySpec.xml(), description, example)

  /**
    * HTML encoded form HTTP message body which will fail to deserialize if a single field is missing/invalid. Use this
    * for server-server communications when you want the server to reject with a BadRequest.
    * This method simply takes a set of form fields.
    */
  def form(fields: FormField[_] with Extractor[Form, _]*): UrlEncodedFormBody = new UrlEncodedFormBody(fields, StrictFormValidator, StrictFormFieldExtractor)

  /**
    * HTML encoded form HTTP message body which deserializes even if fields are missing/invalid. Use this
    * for browser-server communications where you want to give feedback to the user.
    * This method takes a set of form fields, combined with their relevant error messages in case of validation failure.
    */
  def webForm(fields: (FormField[_] with Extractor[Form, _], String)*): UrlEncodedFormBody = new UrlEncodedFormBody(fields.map(_._1), new WebFormValidator(Map(fields: _*)), WebFormFieldExtractor)

  /**
    * MultiPart encoded form HTTP message body which will fail to deserialize if a single field is missing/invalid. Use this
    * for server-server communications when you want the server to reject with a BadRequest.
    * This method simply takes a set of form fields.
    */
  def multiPartForm(fields: FormField[_] with Extractor[Form, _]*): MultiPartFormBody = new MultiPartFormBody(fields, StrictFormValidator, StrictFormFieldExtractor)

  /**
    * MultiPart encoded form HTTP message body which deserializes even if fields are missing/invalid. Use this
    * for browser-server communications where you want to give feedback to the user.
    * This method takes a set of form fields, combined with their relevant error messages in case of validation failure.
    */
  def multiPartWebForm(fields: (FormField[_] with Extractor[Form, _], String)*): MultiPartFormBody = new MultiPartFormBody(fields.map(_._1), new WebFormValidator(Map(fields: _*)), WebFormFieldExtractor)
}
