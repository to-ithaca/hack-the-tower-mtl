# MTL workshop

## Step 0 - Pure evil

### Explaining the code
The code you need to refactor is the calculator in `src/main/scala/mtl/calculator.scala`.  The calculator has buttons, pressed using `press(c: Char)`, and a `screen` which displays a string of the current expression.  If you press the wrong button, the `screen` displays `ERROR` and the calculator is reset.  Let's experiment with the calculator.

In the `hack-the-tower-mtl` directory, enter the sbt console:
```bash
sbt console
```
Create a new calculator
```scala
scala> import mtl._
import mtl._
scala> val c = new Calculator()
c: mtl.Calculator = mtl.Calculator@1a25133c
```
Experiment with it by calling `press` and `screen`

```scala
scala> c.press('3').press('+').press('2').press('=').screen
res0: String = 5
scala> c.press('b').screen
res1: String = ERROR
```

Try typing `c.press('+').screen` twice

```scala
scala> c.press('+').screen
res2: String = +
scala> c.press('+').screen
res3: String = ERROR
```

We performed the same operation twice, but the output was different each time.  This is because the calculator is **mutable** - pressing a button changes its internal state.  In functional programming, this is known as a **side effect**.  The `screen` method is just a getter - it has no side effects.  Your old colleague showed this by [ommitting parentheses](http://docs.scala-lang.org/style/method-invocation.html). 

### Tests
The code for the calculator is tangled and complicated - clearly, your old colleague was in a rush.  Luckily, he left you some regression tests in `src/test/scala/mtl/CalculatorTests.scala`.

Run the tests using:
```bash
sbt test
```

At this point, they should all pass.  Take a closer look at the tests to see the calculator's desired behaviour.

### Refactoring
We're about to write a new, more functional implementation of the calculator.  We're going to create a few new files at this point, so remember that [multi-unit files should have a lowecase first letter](http://docs.scala-lang.org/style/files.html).

Let's create an interface for all calculators to follow.

- In a new file `src/main/scala/mtl/Calculator.scala` type:
```scala
trait Calculator {
   def press(c: Char): Calculator
   def screen: String
}
```

The old scripted calculator should conform to this interface. 

 - Rename `calculator.scala` to `evilCalculator.scala`.
 - In `evilCalculator.scala` rename the calculator to `EvilCalculator`.
```scala
class EvilCalculator extends Calculator {
   ...
}
```
Yes, it certainly lives up to it's name.

Let's now create the functional implementation.

 - In a new file `src/main/scala/mtl/FriendlyCalculator.scala` type:
```scala
class FriendlyCalculator extends Calculator {
   def press(c: Char): Calculator = ???
   def screen: String = ???
}
```

We also need to alter the tests to test both calculators.  

 - Rename `CalculatorTests.scala` to `calculatorTests.scala`.
 - Turn `CalculatorTests` into a trait.
```scala
trait CalculatorTests extends FunSpec with Matchers {
   def calculator: Calculator
   ...
}
```
 - Create `EvilCalculatorTests` and `FriendlyCalculatorTests` inheriting from it.
```scala
class EvilCalculatorTests extends CalculatorTests {
   def calculator: Calculator = new EvilCalculator()
}
class FriendlyCalculatorTests extends CalculatorTests {
   def calculator: Calculator = new FriendlyCalculator()
}
```

The code should now compile with `sbt compile`.  The compiler will interpret `???` as a valid function implementation, which is useful when we want to write method signatures, but not fill them in just yet.  The tests will fail since `FriendlyCalculator` has missing method implementations.

### Summary
In this step, you should have

 1. gained a good grasp of what the functional calculator needs to do
 2. written an interface for all calculators, and an empty implementation of the functional calculator
 3. written tests for the functional calculator

Now checkout step 1 to start refactoring properly
```bash
git checkout step-1-referential-transparency
```
