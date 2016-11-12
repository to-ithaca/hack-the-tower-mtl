package mtl

import cats._
import cats.data._
import cats.implicits._

class FriendlyCalculator extends Calculator {

  private var _state: CalcState = FunCalculator.empty 
  private var _screen: String = ""

  def press(c: Char): Calculator = FunCalculator.press(c).runS(_state) match {
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

  type MStack[A] = StateT[CalcError Either ?, CalcState, A]

  val empty: CalcState = CalcState(Num(0), "")

  def press(c: Char): MStack[Unit] = for {
    s <- liftEither(parse(c))
    _ <- s match {
      case Number(i) => liftState(calc(i) >> write(i.show))
      case o: BinOp =>  calc(o) >> liftState(write(o.show))
      case Equals =>  liftState(equals)
    }
  } yield ()

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

  private def calc(i: Int): State[CalcState, Unit] = State.modify(cs =>
    cs.copy(expr = cs.expr match {
      case Num(c) => Num(c * 10 + i)
      case NumOp(p, o) => NumOpNum(p, o, i)
      case NumOpNum(p, o, c) => NumOpNum(p, o, c * 10 + i)
    }))

  private def calc(o: BinOp): MStack[Unit] = for {
    cs <- liftState(State.get)
    _ <- cs.expr match {
      case Num(n) =>  liftState(State.set(cs.copy(expr = NumOp(n, o))))
      case NumOp(n, p) => liftEither(Left(ConsecutiveOpError(p, o)))
      case NumOpNum(p, po, n) => liftState(State.set(cs.copy(expr = NumOp(binop(p, po, n), o))))
    }
  } yield ()

  private def binop(p: Int, o: BinOp, n: Int): Int = o match {
    case Plus => p + n
    case Minus => p - n
  }

  private def write(s: String): State[CalcState, Unit] = State.modify(cs => 
    cs.copy(display = cs.display + s))

  private def equals: State[CalcState, Unit] = State.modify { cs =>
    val value = cs.expr match {
      case Num(i) => i
      case NumOp(p, o) => binop(p, o, 0)
      case NumOpNum(p, o, n) => binop(p, o, n)
    }
    CalcState(Num(value), value.show)
  }

  private def liftEither[E <: CalcError, A](e: E Either A): MStack[A] =
    StateT.lift(e.leftWiden[CalcError])

  private def liftState[A](s: State[CalcState, A]): MStack[A] =
    s.transformF(a => Right(a.value))
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
