package lens

import io.fintrospect.parameters.ParamType

/**
  * Represents a uni-directional extraction of a list of entities from a target.
  */
trait MultiLensSpec[IN, OUT] {
  def defaulted(name: String, default: List[OUT], description: String = null): Lens[IN, List[OUT]]

  def optional(name: String, description: String = null): Lens[IN, Option[List[OUT]]]

  def required(name: String, description: String = null): Lens[IN, List[OUT]]
}

/**
  * Represents a bi-directional extraction of a list of entities from a target, or an insertion into a target.
  */
trait BiDiMultiLensSpec[IN, OUT] extends MultiLensSpec[IN, OUT] {
  override def defaulted(name: String, default: List[OUT], description: String): BiDiLens[IN, List[OUT]]

  override def optional(name: String, description: String): BiDiLens[IN, Option[List[OUT]]]

  override def required(name: String, description: String): BiDiLens[IN, List[OUT]]
}


class LensGet[IN, MID, OUT] private(private val rootFn: (String, IN) => List[MID], private val fn: (MID) => OUT) {

  def apply(name: String): (IN) => List[OUT] = (target: IN) => rootFn(name, target).map(fn)

  def map[NEXT](nextFn: (OUT) => NEXT): LensGet[IN, MID, NEXT] = new LensGet[IN, MID, NEXT](rootFn, (i: MID) => nextFn(fn(i)))
}

object LensGet {
  def apply[IN, OUT](rootFn: (String, IN) => List[OUT]): LensGet[IN, OUT, OUT] = new LensGet(rootFn, (i: OUT) => i)
}


/**
  * Represents a uni-directional extraction of an entity from a target.
  */
class LensSpec[IN, MID, OUT](protected val location: String, protected val paramMeta: ParamType, private val get: LensGet[IN, MID, OUT]) {

  /**
    * Create another LensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be
    * used to extract the final type from a target.
    */
  def map[NEXT](nextIn: (OUT) => NEXT) = new LensSpec(location, paramMeta, get.map(nextIn))


  /**
    * Make a concrete Lens for this spec that falls back to the default value if no value is found in the target.
    */
  def defaulted(name: String, default: OUT, description: String = null): Lens[IN, OUT] =
    new Lens(Meta(false, location, paramMeta, name, description), it => {
      val out = get(name)(it)
      if (out.isEmpty) default else out.head
    })

  //
  ///**
  //* Make a concrete Lens for this spec that looks for an optional value in the target.
  //*/
  //open fun optional(name: String, description: String? = null): Lens<IN, OUT?> {
  //val meta = Meta(false, location, paramMeta, name, description)
  //val getLens = get(name)
  //return Lens(meta, { getLens(it).let { if (it.isEmpty()) null else it.first() } })
  //}
  //
  ///**
  //* Make a concrete Lens for this spec that looks for a required value in the target.
  //*/
  //open fun required(name: String, description: String? = null): Lens<IN, OUT> {
  //val meta = Meta(true, location, paramMeta, name, description)
  //val getLens = get(name)
  //return Lens(meta, { getLens(it).firstOrNull() ?: throw LensFailure(Missing(meta)) })
  //}
  //
  //open val multi = object : MultiLensSpec<IN, OUT> {
  ///**
  //  * Make a concrete Lens for this spec that falls back to the default list of values if no values are found in the target.
  //  */
  //override fun defaulted(name: String, default: List<OUT>, description: String?): Lens<IN, List<OUT>> {
  //val meta = Meta(false, location, paramMeta, name, description)
  //val getLens = get(name)
  //return Lens(meta, { getLens(it).let { if (it.isEmpty()) default else it } })
  //}
  //
  ///**
  //  * Make a concrete Lens for this spec that looks for an optional list of values in the target.
  //  */
  //override fun optional(name: String, description: String?): Lens<IN, List<OUT>?> {
  //val meta = Meta(false, location, paramMeta, name, description)
  //val getLens = get(name)
  //return Lens(meta, { getLens(it).let { if (it.isEmpty()) null else it } })
  //}
  //
  ///**
  //  * Make a concrete Lens for this spec that looks for a required list of values in the target.
  //  */
  //override fun required(name: String, description: String?): Lens<IN, List<OUT>> {
  //val meta = Meta(true, location, paramMeta, name, description)
  //val getLens = get(name)
  //return Lens(meta, { getLens(it).let { if (it.isEmpty()) throw LensFailure(Missing(meta)) else it } })
  //}
  //}
}

