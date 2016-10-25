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
    s <- parse[MStack](c)
    _ <- s match {
      case Number(i) => calc[MStack](i) >> write[MStack](i.show)
      case o: BinOp =>  calc[MStack](o) >> write[MStack](o.show)
      case Equals =>  equals[MStack]
    }
  } yield ()

  private def parse[F[_]](c: Char)(implicit M: MonadError[F, CalcError]): F[Symbol] = c match {
    case '+' => M.pure(Plus)
    case '-' => M.pure(Minus)
    case '=' => M.pure(Equals)
    case o => try {
      M.pure(Number(Integer.parseInt(o.toString)))
    } catch {
      case e: NumberFormatException => M.raiseError(ParseError(e))
    }
  }

  private def calc[F[_]](i: Int)(implicit M: MonadState[F, CalcState]): F[Unit] = M.modify(cs =>
    cs.copy(expr = cs.expr match {
      case Num(c) => Num(c * 10 + i)
      case NumOp(p, o) => NumOpNum(p, o, i)
      case NumOpNum(p, o, c) => NumOpNum(p, o, c * 10 + i)
    }))

  private def calc[F[_]](o: BinOp)(implicit ME: MonadError[F, CalcError], MS: MonadState[F, CalcState]): F[Unit] = MS.flatMap(MS.get) { cs =>
    cs.expr match {
      case Num(n) =>  MS.set(cs.copy(expr = NumOp(n, o)))
      case NumOp(n, p) => ME.raiseError(ConsecutiveOpError(p, o))
      case NumOpNum(p, po, n) => MS.set(cs.copy(expr = NumOp(binop(p, po, n), o)))
    }}

  private def binop(p: Int, o: BinOp, n: Int): Int = o match {
    case Plus => p + n
    case Minus => p - n
  }

  private def write[F[_]](s: String)(implicit M: MonadState[F, CalcState]): F[Unit] = 
    M.modify(cs => cs.copy(display = cs.display + s))

  private def equals[F[_]](implicit M: MonadState[F, CalcState]): F[Unit] = M.modify { cs =>
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
