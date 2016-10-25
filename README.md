# MTL workshop

## Step 1 - Referential Transparency

Now you've set up the basic structure, you can get to grips with writing a functional solution.  The `FriendlyCalculator` still needs to be mutable - we should preserve this behaviour when refactoring.  Create an object `FunCalculator` for the functional code to live in.
```
object FunCalculator {}
```
Looking through the `EvilCalculator`, you see that the input is first parsed and then processed. The parsing stage can fail if the input is invalid. A functional definition for parse can then be:
```
def parse(c: Char): ParseError Either Symbol = ???
```
An `Either`, or disjunction, represents a construct that contains one of two values.  In this case, it contains a valid `Symbol` or a `ParseError`.

The `ParseError` itself represents something that can go wrong in our functional calculator.

```
sealed trait CalcError
case class ParseError(error: NumberFormatException) extends CalcError
```

After parsing, the processing stage changes depending on what the `Symbol` is. A `Number`, `+` or `-` is added onto an expression, while `=` evaluates the expression.  Create a trait `Symbol` for valid inputs.  Add another trait `BinOp` to distinguish binary operators from numbers.
```
sealed trait Symbol
case class Number(i: Int) extends Symbol
sealed trait BinOp extends Symbol
case object Plus extends BinOp
case object Minus extends BinOp
case object Equals extends Symbol
```
When the need arises, new binary operators can be easily added.

The internal calculator state is made up of an expression and a display
```
case class CalcState(expr: Expr, display: String)
```

Determinig what `Expr` should look like is somewhat difficult.  The `EvilCalculator` eagerly evaluates operators.  As soon as it knows it can evaluate a binary operation on two numbers, it does so, and stores the resulting number.
So if the user types `23+45-`, the calculator stores:

TODO table of state for 23 + 45

This can be summarised as three different states.  If the user is typing a number, if the user has just typed an operator, or if the user is typing a number after an operator.
```
sealed trait Expr
case class Num(i: Int) extends Expr
case class NumOp(i: Int, op: BinOp) extends Expr
case class NumOpNum(l: Int, op: BinOp, r: Int) extends Expr
```

`Number` symbols are appended on the integers of the `Expr`.  `BinOp` symbols may calculate a new `Expr`, but typing two consecutive `BinOp` symbols should result in an error.
This is summarised in the following functions:
```
def calc(i: Int)(cs: CalcState): CalcState = ???
def calc(o: BinOp)(cs: CalcState): ConsecutiveOpError Either CalcState = ???
```

Symbols should also be written to the display.

```
def write(s: String)(cs: CalcState): CalcState = ???
```

An `Equals` symbol should evaluate the current expression, and set the display to the result.
```
def equals(cs: CalcState): CalcState = ???
```

The functional sugnature for `press` is then:
```
def press(c: Char)(cs: CalcState): CalcError Either CalcState = ??? 
```

We ought to add a value for the empty `CalcState`.  This should have a number of 0 and an empty display string.

```
val empty: CalcState = CalcState(Num(0), "")
```

In summary, the method sugnatures for `FunCalculator` should be:

```
object FunCalculator {

  val empty: CalcState = CalcState(Num(0), "")

  def press(c: Char)(cs: CalcState): CalcError Either CalcState = ??? 

  private def parse(c: Char): ParseError Either Symbol = ???

  private def calc(i: Int)(cs: CalcState): CalcState = ???
  private def calc(o: BinOp)(cs: CalcState): ConsecutiveOpError Either CalcState = ???

  private def write(s: String)(cs: CalcState): CalcState = ???

  private def equals(cs: CalcState): CalcState = ???
}

```

Try filling in the methods yourself.

The `FriendlyCalculator` can then have a mutable `CalcState` and `screen`.  Whenever an error is returned, the screen is set to =ERROR=.

```
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

```
