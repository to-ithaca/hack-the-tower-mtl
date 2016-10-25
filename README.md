# MTL workshop

## Step 2 - Stacking Monads

Notice that lots of the functional signatures take in a `CalcState` and return a modified `CalcState`.  This indicates that our system is *stateful* - there is some state that gets modified along out functional pipeline.  Other functions take in a `CalcState` and return a `CalcError Either CalcState`.  This indicates that our system has errors - some of the operations are fallible.  The state and errors in our system are termed effects.  And for each of these effects, there is a monad. The state effect has the `State` monad and the either effect has the `Either` monad.  The monadic properties of these mean that changes can be layered on top of each other using `flatMap` operations.

Note that we're already using the `Either` monad.  To introduce the state monad, instead of returning a function `CalcState => CalcState`, return a `State[CalcState, Unit]`.

For example:
```
private def calc(i: Int)(cs: CalcState): CalcState = ???
```
becomes
```
private def calc(i: Int): State[CalcState, Unit] = ???
```
The function internals can then be wrapped in a `State.modify`.  This handles `calc(i: Int)`, `write` and `equals`.  
Calculating operators has a much more difficult signature:
```
private def calc(o: BinOp)(cs: CalcState): ConsecutiveOpError Either CalcState = ???
```
This has both an `Either` and a `State` effect.  For this, we need to use a `StateT`.  This stacks a State monad on top of an Either monad, such that flatMap operates on the state / either combination.  Our full stack is:

```
type MStack[A] = StateT[CalculatorError Either ?, CalcState, A]
```
We're using `kind-projector` to write the stack cleanly.

The states and eithers need to be lifted into this stack:

```
private def liftEither[E <: CalcError, A](e: E Either A): MStack[A] = 
    StateT.lift(e.leftWiden[CalcError])

private def liftState[A](s: State[CalcState, A]): MStack[A] = 
    s.transformF(a => Right(a.value))
```

The `FriendlyCalculator` can then run the stack produced by `press` to get the next state.
