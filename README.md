# MTL workshop

## Step 0 - Pure evil

The code you need to refactor is that of a basic calculator. The calculator has buttons, pressed using `press`, and a visual display of expressions shown using `screen`.  Unavoidably, the code is mutable - pressing a button changes the current state.  And if you press the wrong button, the `screen` displas `ERROR` and the calculator is reset.

### Rolling up your sleeves
Brace yourself for the task ahead.  You'll need to preserve the old implementation while you write the new one, so create an interface for all calculators.
```
trait Calculator {
   def press(c: Char): Calculator
   def screen: String
}
```
The old scripted calculator should conform to this interface.  It can't keep the same name, so let's rename it to `EvilCalculator`.
```
class EvilCalculator extends Calculator {
   ...
}
```
Yes, it certainly lives up to it's name.

Let's create a new `FriendlyCalculator` to contain the functional implementation.
```
class FriendlyCalculator extends Calculator {
   def press(c: Char): Calculator = ???
   def screen: String = ???
}
```

We also need to alter the tests to test both calculators.  Turn `CalculatorTests` into a trait, and create `EvilCalculatorTests` and `FriendlyCalculatorTests` inheriting from it.
```
trait CalculatorTests extends FunSpec with Matchers {
   def calculator: Calculator
   ...
}

class EvilCalculatorTests extends CalculatorTests {
   def calculator: Calculator = new EvilCalculator()
}

class FriendlyCalculatorTests extends CalculatorTests {
   def calculator: Calculator = new FriendlyCalculator()
}
```

The code should now compile with `sbt compile`, but the tests will fail since `FriendlyCalculator` has missing method implementations.