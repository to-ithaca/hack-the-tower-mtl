# MTL workshop

## Step 1 - Referential Transparency

Now you've set up the basic structure, you can get to grips with coding a functional solution.  The `FriendlyCalculator` still needs to be mutable - we should preserve this behaviour when refactoring.

 - In `FriendlyCalculator.scala` create:
```
object FunCalculator {}
```
We'll put the functional code within the `FunCalculator`.

### A note on functional code
At minumum, a pure functional code block must be:

 - **pure** - evaluating the same expression will always give the same result.
Consider the following code block:
```
scala> val c = new EvilCalculator()
c: mtl.Calculator = mtl.Calculator@1a25133c
scala> c.press('+').screen
res0: String = +
scala> c.press('+').screen
res1: String = ERROR
```
`c.press('+').screen` is impure, because it gives a different result each time it is called.

 - have no **side effects** - a function call cannot mutate its arguments, or perform any IO operations.
 
An expression which is pure and has no side effects is then **referentially transparent**.  This means it can be replaced by its result anywhere in the code without altering the result of the program.  Referentially transparent code is much easier to reason about, as the same inputs always result in the same outputs.

We will keep all referentially transparent code in the `FunCalculator`.  The `FriendlyCalculator` will contain the only mutable state.

### Refactoring

We want to refactor the internals of the calculator to suit a functional pipeline.  Let's take a look at the `EvilCalculator`.  Luckily, it only has two public methods, `press` and `screen`.  If it exposed more of its mutable state, refactoring it would be much more difficult.  By taking a quick look at the code, we see that the input character is parsed within the `press` method, before being used to calculate a change on the internal state.  Either the charater, result or `ERROR` is written to the screen.  We'll walk through functional signatures for each of these stages.

#### Parsing
The character input of `press` is matched against '+', '-', '=', and an attempt is made to parse it into a number.  The parsing stage can fail if the input is invalid.

Write the following definition of parse:
```
def parse(c: Char): ParseError Either Symbol = ???
```
An `Either`, or disjunction, represents a construct that contains one of two values.  In this case, it contains a valid `Symbol` or a `ParseError`.  A `Symbol` can represent a '+', '-', '=' or number.  The `ParseError` itself represents something that can go wrong in our functional calculator.  Note that we've not chosen to use `Throwable` or `Exception`, but used a very specific error.  This makes it easier for anyone else to reason about what `parse` does, and what can go wrong with it.  Other things can go wrong within our application.  We can represent these with an error trait.

Create a trait `CalcError` and a `ParseError` which extends from it:

```
sealed trait CalcError
case class ParseError(error: NumberFormatException) extends CalcError
```
### Symbols

The symbols resulting from parsing are binary operators ('+' or '-'), '=' or single digit numbers.
Create a `Symbol` trait to represent these:
```
sealed trait Symbol
case class Number(i: Int) extends Symbol
sealed trait BinOp extends Symbol
case object Plus extends BinOp
case object Minus extends BinOp
case object Equals extends Symbol
```

Having a `BinOp` trait means that we can add new binary operators with relative ease.

### Calculation

After parsing, the `EvilCalculator` eagerly evaluates operators.  As soon as an operator has a fully typed number to the left and right, the operator is evaluated and its result is put on the left.  If the user types `3+45-`, the calculator calculates `3+45` on receiving `-`, and stores `48-` in its left hand option.  Instead of using `Option` for the left number, right number and operator, we can use some form of expression `Expr`.

Create the trait `Expr` with the following subclasses:
```
sealed trait Expr
case class Num(i: Int) extends Expr
case class NumOp(i: Int, op: BinOp) extends Expr
case class NumOpNum(l: Int, op: BinOp, r: Int) extends Expr
```

The internal calculator state is made up of an expression and a display.
Create a trait `CalcState` to represent the full internal calculator state:
```
case class CalcState(expr: Expr, display: String)
```

`Number` and operator symbols result in different changes to the `Expr`.  `Number` symbols are appended on the integers of the `Expr`.  `BinOp` symbols may calculate a new `Expr`, but typing two consecutive `BinOp` symbols should result in an error.
Summarise this in the following function signatures:
```
def calc(i: Int)(cs: CalcState): CalcState = ???
def calc(o: BinOp)(cs: CalcState): ConsecutiveOpError Either CalcState = ???
```

Add a `ConsecutiveOpError` class:
```
case class ConsecutiveOpError(prev: BinOp, next: BinOp) extends CalcError
```
### Writing

If a symbol is processed successfully, it is written to the display.
Add a function `write` with the following signature:
```
def write(s: String)(cs: CalcState): CalcState = ???
```

An `Equals` symbol should evaluate the current expression, and set the display to the result.
Add a function `equals` with the following signature:
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

### Our version
TODO make use of cats's either and flatMap.  A brief introduction to monads.

### FriendlyCalculator

The `FriendlyCalculator` can then have a mutable `CalcState` and `screen`.  Whenever an error is returned, the screen is set to `ERROR`.

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

### Next Steps

Checkout step 2 with:
```
git checkout -f step-2-stacking-monads
```
