package mtl

import cats._
import cats.data._
import cats.implicits._

class FriendlyCalculator extends Calculator {

  private var _state: CalcState = FunCalculator.empty 
  private var _screen: String = ""

  def press(c: Char): Calculator = FunCalculator.press(c)(_state) match {
    case Right(next) => 
      _state = next
      _screen = next.display
      this
    case Left(_) =>
      _state = FunCalculator.empty
      _screen = "ERROR"
      this
  }

  def screen: String = _screen
}

object FunCalculator {

  val empty: CalcState = CalcState(Num(0), "")

  def press(c: Char)(cs: CalcState): CalcError Either CalcState = 
    parse(c).leftWiden[CalcError].flatMap {
      case Number(i) => Right(write(i.show)(calc(i)(cs)))
      case o: BinOp => (calc(o)(cs)).map(write(o.show))
      case Equals => Right(equals(cs))
    }

  private def parse(c: Char): ParseError Either Symbol = c match {
    case '+' => Right(Plus)
    case '-' => Right(Minus)
    case '=' => Right(Equals)
    case o => try {
      Right(Number(Integer.parseInt(o.toString)))
    } catch {
      case e: NumberFormatException => Left(ParseError(e))
    }
  }


  private def calc(i: Int)(cs: CalcState): CalcState = 
    cs.copy(expr = cs.expr match {
      case Num(c) => Num(c * 10 + i)
      case NumOp(p, o) => NumOpNum(p, o, i)
      case NumOpNum(p, o, c) => NumOpNum(p, o, c * 10 + i)
    })

  private def calc(o: BinOp)(cs: CalcState): ConsecutiveOpError Either CalcState = 
    cs.expr match {
      case Num(n) =>  Right(cs.copy(expr = NumOp(n, o)))
      case NumOp(n, p) => Left(ConsecutiveOpError(p, o))
      case NumOpNum(p, po, n) => Right(cs.copy(expr = NumOp(binop(p, po, n), o)))
    }

  private def binop(p: Int, o: BinOp, n: Int): Int = o match {
    case Plus => p + n
    case Minus => p - n
  }

  private def write(s: String)(cs: CalcState): CalcState = 
    cs.copy(display = cs.display + s)

  private def equals(cs: CalcState): CalcState = {
    val value = cs.expr match {
      case Num(i) => i
      case NumOp(p, o) => binop(p, o, 0)
      case NumOpNum(p, o, n) => binop(p, o, n)
    }
    CalcState(Num(value), value.show)
  }
}

sealed trait Symbol
case class Number(i: Int) extends Symbol
sealed trait BinOp extends Symbol
case object Plus extends BinOp
case object Minus extends BinOp
case object Equals extends Symbol

object BinOp {

  implicit val show: Show[BinOp] = new Show[BinOp] {
    def show(o: BinOp): String = o match {
      case Plus => "+"
      case Minus => "-"
    }
  }
}

case class CalcState(expr: Expr, display: String)

sealed trait Expr
case class Num(i: Int) extends Expr
case class NumOp(i: Int, o: BinOp) extends Expr
case class NumOpNum(l: Int, o: BinOp, r: Int) extends Expr

sealed trait CalcError extends Throwable
case class ParseError(error: NumberFormatException) extends CalcError
case class ConsecutiveOpError(previous: BinOp, current: BinOp) extends CalcError
