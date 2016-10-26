# MTL workshop

## Step 1 - Referential Transparency

We'll now start coding a functional solution.  The `FriendlyCalculator` still needs to be mutable, we should preserve this behaviour when refactoring.

 - In `FriendlyCalculator.scala` create:
```scala
object FunCalculator {}
```
We'll put the functional code within the `FunCalculator`. 

### Functional code
At minumum, a functional code block must be:

 - **pure**:  evaluating the same expression will always give the same result.
Consider the following code block:
```scala
scala> val c = new EvilCalculator()
c: mtl.Calculator = mtl.Calculator@1a25133c
scala> c.press('+').screen
res0: String = +
scala> c.press('+').screen
res1: String = ERROR
```
`c.press('+').screen` is impure, because it gives a different result each time it is called.

 - have no **side effects**: a function call cannot mutate its arguments, or perform any IO operations.
 
An expression which is pure and has no side effects is then **referentially transparent**.  It can be replaced by its result anywhere in the code without altering the result of the program.  Referentially transparent code is much easier to reason about, as the same inputs always result in the same outputs.

We will keep all referentially transparent code in the `FunCalculator`.  The `FriendlyCalculator` will contain the only mutable state.

### Function Signatures

We need to write a functional version of the `press` method on the calculator.  This can have 3 stages:

 1. Parsing an input `Char` into a valid symbol
 2. Appending the symbol onto an expression
 3. Writing the symbol to the display
 
We'll go through the function signatures for each of these stages.

#### Parsing
The character input of `press` is matched against `+`, `-`, `=`, and an attempt is made to parse it into a number.  The parsing stage can fail if the input is invalid.

Write the following definition of parse:
```scala
def parse(c: Char): ParseError Either Symbol = ???
```
An `Either`, or disjunction, represents a construct that contains one of two values.  In this case, it contains a valid `Symbol` or a `ParseError`.  A `Symbol` can represent a `+`, `-`, `=` or number.

The `ParseError` itself represents something that can go wrong in our functional calculator.  Note that we've not chosen to use `Throwable` or `Exception`, but used a very specific error.  This makes it easier for anyone else to reason about what `parse` does, and what can go wrong with it.

Other things can go wrong within our application.  We can represent these with an error trait.

Create a trait `CalcError` and a `ParseError` which extends from it:

```scala
sealed trait CalcError
case class ParseError(error: NumberFormatException) extends CalcError
```
### Symbols

The symbols resulting from parsing are the binary operators `+` or `-`, `=` or single digit numbers.

Create a `Symbol` trait to represent these:
```scala
sealed trait Symbol
case class Number(i: Int) extends Symbol
sealed trait BinOp extends Symbol
case object Plus extends BinOp
case object Minus extends BinOp
case object Equals extends Symbol
```

Having a `BinOp` trait means that we can add new binary operators with ease.

### Calculation

After parsing, the `EvilCalculator` eagerly evaluates operators.  As soon as an operator has a fully typed number to the left and right, the operator is evaluated and its result is put on the left.  For example, if the user types `3+45-`, the calculator calculates `3+45` on receiving `-`, and stores `48-`.  We can use some form of expression `Expr` to store the left and right numbers.

Create the trait `Expr` with the following subclasses:
```scala
sealed trait Expr
case class Num(i: Int) extends Expr
case class NumOp(i: Int, op: BinOp) extends Expr
case class NumOpNum(l: Int, op: BinOp, r: Int) extends Expr
```

The internal calculator state is made up of an expression and a display.

Create a trait `CalcState` to represent the full internal calculator state:
```scala
case class CalcState(expr: Expr, display: String)
```

`Number` and operator symbols result in different changes to the `Expr`.

 - `Number` symbols are appended on the integers of the `Expr`.
 - `BinOp` symbols may calculate a new `Expr`, but typing two consecutive `BinOp` symbols should result in an error.

Summarise this in the following function signatures:
```scala
def calc(i: Int)(cs: CalcState): CalcState = ???
def calc(o: BinOp)(cs: CalcState): ConsecutiveOpError Either CalcState = ???
```

Add a `ConsecutiveOpError` class:
```scala
case class ConsecutiveOpError(prev: BinOp, next: BinOp) extends CalcError
```
### Writing

If a symbol is processed successfully, it is written to the display.

Add a function `write` with the following signature:
```scala
def write(s: String)(cs: CalcState): CalcState = ???
```

An `Equals` symbol should evaluate the current expression, and set the display to the result.

Add a function `equals` with the following signature:
```scala
def equals(cs: CalcState): CalcState = ???
```

The functional sugnature for `press` is then:
```scala
def press(c: Char)(cs: CalcState): CalcError Either CalcState = ??? 
```

We ought to add a value for the empty `CalcState`.  This should have a number of 0 and an empty display string.

```scala
val empty: CalcState = CalcState(Num(0), "")
```

#### Summary
The method sugnatures within `FunCalculator` should be:

```scala
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

### Our implementations

We'll use [cats](https://github.com/typelevel/cats) as part of our implementation.  Include cats in your `build.sbt`:
```scala
libraryDependencies += "org.typelevel" %% "cats-core" % "0.8.0"
```

In `FriendlyCalculator.scala` import cats:
```scala
import cats._
import cats.data._
import cats.implicits._
```

#### Parse
For parse, we pattern match on the input character, and return a valid symbol on the right.  In the case that the symbol is not `+`, `-`, `=` or a valid integer number, we return a `ParseError` on the left.

``` scala
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
```

#### Calc
For `calc(i: Int)` we pattern match on `expr` and append the input integer.  If there is already an integer at the end of `expr`, we multiply it by 10.  So if the user has typed 2 and then types 3, the stored integer is 23.
``` scala
private def calc(i: Int)(cs: CalcState): CalcState = 
    cs.copy(expr = cs.expr match {
      case Num(c) => Num(c * 10 + i)
      case NumOp(p, o) => NumOpNum(p, o, i)
      case NumOpNum(p, o, c) => NumOpNum(p, o, c * 10 + i)
})
```

For `calc(o: BinOp)` we pattern match on `expr`.
 - If `expr` is in state `Num`, we append the input operator.
 - If an operator has already been typed, `expr` is in state `NumOp`.  In this case, we return a `ConsecutiveOpError` on the left.
 - If `expr` is in state `NumOpNum`, the previously stored binary operator is evaluated on the stored `p` and `n` numbers.  The result is placed to the left of the binary operator in state `NumOp`.
``` scala
private def calc(o: BinOp)(cs: CalcState): ConsecutiveOpError Either CalcState = 
  cs.expr match {
    case Num(n) =>  Right(cs.copy(expr = NumOp(n, o)))
    case NumOp(n, p) => Left(ConsecutiveOpError(p, o))
    case NumOpNum(p, po, n) => Right(cs.copy(expr = NumOp(binop(p, po, n), o)))
}
```

The function `binop` evaluates a binary operator:

``` scala
private def binop(p: Int, o: BinOp, n: Int): Int = o match {
    case Plus => p + n
    case Minus => p - n
}
```

#### Write

For `write`, we simply append the input onto the display

``` scala
private def write(s: String)(cs: CalcState): CalcState = 
  cs.copy(display = cs.display + s)
```

#### Equals

For equals, we pattern match on the `expr`:

 - If `expr` is a `Num`, we return its integer value
 - If it is a `NumOp`, we perform the binary operation with the right side as zero
 - If it is a `NumOpNum`, we perform the binary operation using both of the stored integers.

``` scala
private def equals(cs: CalcState): CalcState = {
    val value = cs.expr match {
      case Num(i) => i
      case NumOp(p, o) => binop(p, o, 0)
      case NumOpNum(p, o, n) => binop(p, o, n)
    }
    CalcState(Num(value), value.show)
}
```

#### Show
You'll notice a new function `show` here.  This function is present on the cats `Show` typeclass, which indicates that values can be displayed as strings.  The `Show` typeclass instance for `Int` is defined in the `cats` library and imported using `import cats.implicits._`.

We'll need to write a `Show` typeclass instance for binary operators for use later, since these also need to be displayed.

We create a new object `Binop`:
``` scala
object BinOp {
  implicit val show: Show[BinOp] = new Show[BinOp] {
    def show(o: BinOp): String = o match {
      case Plus => "+"
      case Minus => "-"
    }
  }
}
```

Because the implicit `Show[BinOp]` is declared within the `BinOp` companion object, [the compiler can find it anywhere in the codebase](http://docs.scala-lang.org/tutorials/FAQ/finding-implicits.html).

#### Press

The implementation of `press` must combine `parse`, `calc`, `write` and `equals`.

``` scala
def press(c: Char)(cs: CalcState): CalcError Either CalcState = 
    parse(c).leftWiden[CalcError].flatMap {
      case Number(i) => Right(write(i.show)(calc(i)(cs)))
      case o: BinOp => (calc(o)(cs)).map(write(o.show))
      case Equals => Right(equals(cs))
}
```

There are several parts you may not have seen before.

 - `leftWiden` widens an `Either[ParseError, A]` into an `Either[CalcError, A]`.
 - `flatMap` is an operation on **monads**.

#### A note about Monads
There are many worderful tutorials out there about monads, so we won't go into detail on them here.  For our purposes, a monad instance is something which can be flatMapped.  i.e. it declares the function:

``` scala
def flatMap[F[_], A, B](fa: F[A], f: a => F[B]): F[B]
```

Here `F[_]` represents a monadic datatype, in our case an `Either`.
The `flatMap` for `Either` has a signature similar to:

``` scala
def flatMap[E, A, B](fa: Either[E, A], f: A => Either[E, B]): Either[E, B]
```
The monad instance for `Either` is provided by cats, and imported with `import cats.implicits._`.  As an excercise, try writing the `flatMap` function yourself.

In `press`, we `flatMap` on an `Either[CalcError, Symbol]` using a function `Symbol => Either[CalcError, State]`.  We pattern match on the symbol:

 - If it is a number, we append it onto `expr` using the `calc` function.
 - If it is a `BinOp`, we modify `expr` using the `cal` function.  If this succeeds, we `write` it to the display.
 - If it is an `Equals`, we evaluate the state using `equals`.


### FriendlyCalculator

Now that we have a functional definition for `press`, along with a `display` as part of our state, we can write the mutable code within the `FriendlyCalculator`.  It has a `var` for the state and screen.  Whenever an error is returned, the screen is set to `ERROR` and the state is reset.

```scala
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

### Summary
In this step, you should have:

 1. Understood the basic principles of functional programming
 2. Written function signatures for the functional calculator
 3. Written functional implementations to match those signatures

### Next Steps

Checkout step 2 with:
```
git checkout -f step-2-stacking-monads
```
