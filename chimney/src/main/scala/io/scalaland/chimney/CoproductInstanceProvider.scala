package io.scalaland.chimney

import shapeless.labelled.{FieldType, field}
import shapeless._

trait CoproductInstanceProvider[Label <: Symbol, FromT, ToLG <: Coproduct, Modifiers <: HList] {

  def provide(src: FieldType[Label, FromT], modifiers: Modifiers): ToLG
}

object CoproductInstanceProvider extends CoproductInstanceProviderDerivation {

  final def provide[Label <: Symbol, FromT, To, ToLG <: Coproduct, Modifiers <: HList](
    src: FieldType[Label, FromT],
    clz: Class[To],
    modifiers: Modifiers
  )(implicit lg: LabelledGeneric.Aux[To, ToLG], cip: CoproductInstanceProvider[Label, FromT, ToLG, Modifiers]): To =
    lg.from(cip.provide(src, modifiers))
}

trait CoproductInstanceProviderDerivation extends LowPriorityCoproductInstanceProvider {

  implicit final def matchingObjCase[ToLG <: Coproduct, Label <: Symbol, FromT, TargetT, Modifiers <: HNil](
    implicit sel: ops.union.Selector.Aux[ToLG, Label, TargetT],
    wit: Witness.Aux[TargetT],
    inj: ops.coproduct.Inject[ToLG, FieldType[Label, TargetT]]
  ): CoproductInstanceProvider[Label, FromT, ToLG, Modifiers] =
    (_: FieldType[Label, FromT], _: Modifiers) => inj(field[Label](wit.value))

  implicit final def coproductInstanceCase[ToLG <: Coproduct,
                                           Label <: Symbol,
                                           Inst,
                                           FromT <: Inst,
                                           To,
                                           Modifiers <: HList](
    implicit lg: LabelledGeneric.Aux[To, ToLG]
  ): CoproductInstanceProvider[Label, FromT, ToLG, Modifier.coproductInstance[Inst, To] :: Modifiers] =
    (src: FieldType[Label, FromT], modifiers: Modifier.coproductInstance[Inst, To] :: Modifiers) =>
      lg.to(modifiers.head.f(src))

  implicit final def cconsTailCase[ToLG <: Coproduct, Label <: Symbol, FromT, M <: Modifier, Modifiers <: HList](
    implicit cip: CoproductInstanceProvider[Label, FromT, ToLG, Modifiers]
  ): CoproductInstanceProvider[Label, FromT, ToLG, M :: Modifiers] =
    (src: FieldType[Label, FromT], modifiers: M :: Modifiers) => cip.provide(src, modifiers.tail)
}

trait LowPriorityCoproductInstanceProvider {
  implicit final def matchingTransformerCase[ToLG <: Coproduct, Label <: Symbol, FromT, TargetT, Modifiers <: HNil](
    implicit sel: ops.union.Selector.Aux[ToLG, Label, TargetT],
    transformer: DerivedTransformer[FromT, TargetT, Modifiers],
    inj: ops.coproduct.Inject[ToLG, FieldType[Label, TargetT]]
  ): CoproductInstanceProvider[Label, FromT, ToLG, Modifiers] =
    (src: FieldType[Label, FromT], modifiers: Modifiers) => inj(field[Label](transformer.transform(src, modifiers)))

}
