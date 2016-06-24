package io.fintrospect.parameters

import io.fintrospect.parameters.InvalidParameter.{Invalid, Missing}

import scala.util.{Failure, Success, Try}

/**
  * A parameter is a name-value pair which can be encoded into an HTTP message. Sub-types
  * represent the various places in which values are encoded (eg. header/form/query/path)
  */
trait Parameter {
  val required: Boolean
  val name: String
  val description: Option[String]
  val where: String
  val paramType: ParamType

  override def toString = s"Parameter(name=$name,where=$where,paramType=${paramType.name})"

  protected def extractFrom[T](deserialize: Seq[String] => Try[T],
                     fromInput: Option[Seq[String]]): Extraction[T] =
    fromInput.map {
      v =>
        deserialize(v) match {
          case Success(d) => Extracted(d)
          case Failure(_) => ExtractionFailed(Invalid(this))
        }
    }.getOrElse(if (required) ExtractionFailed(Missing(this)) else NotProvided)
}

/**
  * Parameter location specific utility functions to assist with extraction and binding of values
  */
trait ParameterExtractAndBind[From, B <: Binding] {
  def newBinding(parameter: Parameter, value: String): B
  def valuesFrom(parameter: Parameter, from: From): Option[Seq[String]]
}

abstract class SingleParameter[T, From, B <: Binding](spec: ParameterSpec[T], eab: ParameterExtractAndBind[From, B]) {
  self: Parameter with Bindable[T, B] =>

  override val name = spec.name
  override val description = spec.description
  override val paramType = spec.paramType

  override def -->(value: T) = Seq(eab.newBinding(this, spec.serialize(value)))

  def <--?(from: From) = extractFrom(xs => Try(spec.deserialize(xs.head)), eab.valuesFrom(this, from))
}

abstract class MultiParameter[T, From, B <: Binding](spec: ParameterSpec[T], eab: ParameterExtractAndBind[From, B]) {

  self: Parameter with Bindable[Seq[T], B] =>

  override val name = spec.name
  override val description = spec.description
  override val paramType = spec.paramType

  override def -->(value: Seq[T]) = value.map(v => eab.newBinding(this, spec.serialize(v)))

  def <--?(from: From): Extraction[Seq[T]] = extractFrom(xs => Try(xs.map(spec.deserialize)), eab.valuesFrom(this, from))
}