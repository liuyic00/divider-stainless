package libraryUInt

import stainless.lang._
import stainless.collection._
import stainless.equations._
import stainless.annotation._
import stainless.proof.check

@library
sealed abstract class Bits {
  def asUInt: UInt
}

@library
case class UInt(val value: BigInt, val width: BigInt) extends Bits {
  require(0 < width)
  require(0 <= value && value < Pow2(width))

  def apply(idx: BigInt): Bool = {
    require(0 <= idx && idx < width)
    Bool((value / Pow2(idx)) % 2 == 1)
  }
  def apply(left: BigInt, right: BigInt): UInt = {
      require(right >= 0)
      require(left >= right)
      UInt((value / Pow2(right)) % Pow2(left - right + 1), left - right + 1)
  } ensuring(res => res.width == left - right + 1 && res.value == (this.value / Pow2(right)) % Pow2(left - right + 1))

  def getWidth: BigInt = width
  def asUInt: UInt     = this
  def asBool: Bool = {
    require(width == 1)
    Bool(if (value == 1) true else false)
  }
  def :=(that: UInt): UInt = {
    UInt(that.value % Pow2(this.width), this.width)
  }

  // Unary

  def unary_- : UInt = {
    UInt(
      if (value == 0) 0 else Pow2(width) - value,
      width
    )
  }
  // def unary_~ : UInt = {
  //   def reverseUInt(u: UInt): UInt = {
  //     def f(result: BigInt, width: BigInt, bits: BigInt): BigInt = {
  //       if (width > 0) {
  //         f(result * 2 + bits % 2, width - 1, bits / 2)
  //       } else {
  //         result
  //       }
  //     }
  //     UInt(f(0, u.value, u.width), u.width)
  //   }
  //   def reverseFlipUInt(u: UInt): UInt = {
  //     def f(result: BigInt, width: BigInt, bits: BigInt): BigInt = {
  //       if (width > 0) {
  //         f(result * 2 + (bits + 1) % 2, width - 1, bits / 2)
  //       } else {
  //         result
  //       }
  //     }
  //     UInt(f(0, u.value, u.width), u.width)
  //   }
  //   reverseUInt(reverseFlipUInt(this))
  // }

  // Binary

  def +(that: UInt): UInt = {
    val carryed  = this.value + that.value
    val newWidth = if (this.width > that.width) this.width else that.width
    val limt     = Pow2(newWidth)

    Pow2.Pow2Monotone(newWidth, this.width)
    Pow2.Pow2Monotone(newWidth, that.width)
    assert(limt >= this.value)
    assert(limt >= that.value)
    assert(carryed >= 0)
    assert(carryed - limt < limt)

    UInt(
      if (carryed >= limt) carryed - limt else carryed,
      newWidth
    )
  }
  def -(that: UInt): UInt = {
    val carryed  = this.value - that.value
    val newWidth = if (this.width > that.width) this.width else that.width
    val limt     = Pow2(newWidth)

    Pow2.Pow2Monotone(newWidth, this.width)
    Pow2.Pow2Monotone(newWidth, that.width)
    assert(limt >= this.value)
    assert(limt >= that.value)
    assert(carryed <= this.value && carryed >= -that.value)
    assert(carryed <= limt && carryed >= -limt)

    UInt(
      if (carryed < 0) carryed + limt else carryed,
      newWidth
    )
  }
  def <<(that: UInt): UInt = {
    assert(that.value <= Pow2(that.width) - 1)
    
    Pow2.Pow2Monotone(Pow2(that.width) - 1, that.value)
    assert(Pow2(that.value) <= Pow2(Pow2(that.width) - 1))
    
    {
      Pow2(this.width + Pow2(that.width) - 1) ==:| Pow2.Pow2Mul(this.width + Pow2(that.width) - 1, this.width, Pow2(that.width) - 1) |:
      Pow2(this.width) * Pow2(Pow2(that.width) - 1)
    }.qed
    
    assert(this.value < Pow2(this.width))
    assert(Pow2(that.value) <= Pow2(Pow2(that.width) - 1))
    assert(Pow2(this.width) * Pow2(Pow2(that.width) - 1) == Pow2(this.width + Pow2(that.width) - 1))
    assert(this.value * Pow2(that.value) < Pow2(this.width + Pow2(that.width) - 1))

    UInt(this.value * Pow2(that.value), this.width + Pow2(that.width) - 1)
  } ensuring(res => res.value < Pow2(this.width + that.value))

  // def <<(that: BigInt): UInt = {
  //   UInt(this.value * Pow2(that), this.width + that)
  // }

  // Binary compire
  def ===(that: UInt): Bool = {
    Bool(this.value == that.value)
  }
  def >=(that: UInt): Bool = {
    Bool(this.value >= that.value)
  }
}

@library
object UInt {
  def empty(width: BigInt): UInt = {
    require(0 < width)
    UInt(BigInt(0), width)
  }
  // def :=(that: UInt): Unit = {
  //   val limt     = Pow2(this.width)
  //   this.value = that.value % limt
  // }
}

@library
case class Bool(val value: Boolean) extends Bits {
  def asUInt: UInt = {
    if (value) {
      UInt(1, 1)
    } else {
      UInt(0, 1)
    }
  } ensuring(res => res.width == BigInt(1))

  def asBigInt: BigInt = {
    if (value) {
      BigInt(1)
    } else {
      BigInt(0)
    }
  }

  def unary_! : Bool = {
    Bool(!value)
  }
  def unary_~ : Bool = {
    Bool(!value)
  }

  def &(that: Bool): Bool = {
    Bool(this.value & that.value)
  }
  def |(that: Bool): Bool = {
    Bool(this.value | that.value)
  }
  def ^(that: Bool): Bool = {
    Bool(this.value ^ that.value)
  }
  def &&(that: Bool): Bool = {
    Bool(this.value && that.value)
  }
}

@library
object Bool {
  def empty(): Bool = {
    Bool(false)
  }
}

@library
case class Lit(value: BigInt, width: BigInt) {
  require(0 <= value && value < Pow2(width))
  require(0 < width)
  def U: UInt = UInt(value, width)
}

// set the width of 0.U to 1, not bitLength(0)     
@library
object Lit {
  def apply(value: BigInt): Lit = {
    require(0 <= value)
    if (value == 0) {
      Lit(0, 1)
    } else {
      Pow2.Pow2BitLength(value)
      Lit(value, bitLength(value))
    }
  }
}
