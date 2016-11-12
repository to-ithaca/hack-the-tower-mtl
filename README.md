# MTL workshop

## Step 2 - Stacking Monads

### Functional pipeline features
If we take a closer look at the signatures within `FunCalculator`, we see that they can be binned into roughly 3 categories:

 - functions that result in a failure state, like `parse`.  This failure state is captured in an `Either`
 - functions that alter the `CalcState`, like `calc(i: Int)`, `write` and `equals`
 - functions that have a failure state and alter the `CalcState`, like `calc(o: BinOp)` and `press`
 
We can boil down our functional pipeline into two key features, **state modification** and **failure handling**.  We're going to encompass these features in monads.
We're already familiar with the *Either monad* for layering fallible functions ontop of each other, but what can we do for state?

### Layering state changes
We have multiple functions performing state changes.  We want to perform one on the result of the other.  Consider the following extract from `press`:

```scala
write(i.show)(calc(i)(cs))
```
The state `cs` is inputted into the `calc(i)` function.  It's output is then put straight into the `write(i.show)` function.  This is difficult to read.  We could have an even more complicated example:

``` scala
def foo(cs: CalcState): CalcState = ???
def bar(cs: CalcState): CalcState = ???
def baz(cs: CalcState): (CalcState, Int) = ???
def cow(i: Int)(cs: CalcState): CalcState = ???

def together(cs: CalcState): CalcState = {
    val s0 = foo(cs)
    val s1 = bar(s0)
    val (s2, i) = baz(s1)
    cow(i)(s2)
}
```

It may be easy to reason about the signature, but the code inside is a mess.  It would be great if we could somehow capture these state modifications in some functional construct, so that the state got passed around and modified along with it, a bit like how the `Either` captures the behaviour of an error.

### Musings on state
Let's try and make such a functional construct, let's call it a `StateFunction`.  What would this `StateFunction` have to look like?  It would at least have to represent a function `CalcState => CalcState`.  It would be cool if we could pipe the output of a `StateFunction` into another `StateFunction`, since we seem to need to do that a lot.

``` scala
case class StateFunction(f: CalcState => CalcState) {
  def pipe(next: StateFunction): StateFunction = StateFunction(f andThen next.f)
}
```
Our `fooBar` example above would then just be:

``` scala
def foo: StateFunction = ???
def bar: StateFunction = ???
def baz(cs: CalcState): (CalcState, Int) = ???
def cow(i: Int): StateFunction = ???

def together: StateFunction = StateFunction { cs =>
  val (s, i) = baz(foo.pipe(bar).f(cs))
  cow(i).f(s)
}
```

Well, `foo.pipe(bar)` is good, but the rest is still hard to read.  It would be better if we could make `baz` return some kind of a `StateFunction` instead, and have its result piped to `cow`.

Let's have `StateFunction` represent `CalcState => (CalcState, A)` where `A` is an arbitrary type.  The function `CalcState => CalcState` is just a specific form of this, where `A` is `Unit`.  While we're at it, we can write `pipe` to take in `A => StateFunction[B]`, so we can use the result of `baz` in `cow`.
``` scala
case class StateFunction[A](f: CalcState => (CalcState, A)) {
  def pipe[B](next: A => StateFunction[B]): StateFunction[B] = StateFunction[B] { cs =>
    val (s, a) = f(cs)
    next(a).f(s)
  }
}
```
`fooBar` now looks like:
``` scala
def foo: StateFunction[Unit] = ???
def bar: StateFunction[Unit] = ???
def baz: StateFunction[Int] = ???
def cow(i: Int): StateFunction[Unit] = ???

def together: StateFunction[Unit] = 
  foo.pipe(_ => bar).pipe(_ => baz).pipe(cow)
```

That's much nicer!  We can now see the flow of our program without seeing loads of temporary states.

### Parametricity
Notice that in our `StateFunction` we aren't using any properties of `CalcState` at all.  We're just passing it into functions.  This means that we can generalise the `StateFunction` to work with any state `S`:
```scala
class StateFunction[S, A](f: S => (S, A)) {
...
}
```
Not only does this let us use the `StateFunction` for other states, it also makes it easy to reason about what a `StateFunction` can do.  It can't actually use `S` itself, just pass it along to the functions it wraps.

### A closer look
If we take a closer look at our `StateFunction`, we see that the signature of `pipe` is exactly the same as that of `flatMap`.  This means that it's highly likely that our `StateFunction` is a monad itself!  In fact, with a bit of reworking, it can be made into one.  We don't need to go that far, because **the State monad** already exists in cats, and does the same thing as our `StateFunction`.

### The State Monad

[The State monad](http://typelevel.org/cats/datatypes/state.html) can be imported with `import cats.data._`.  Is has the form `State[S, A]`, where `S` is the state, in this case `CalcState`, and `A` is its result type.

The `State` companion object in cats has useful methods for creating `State` instances from functions.

To introduce the State monad, instead of returning a function `CalcState => CalcState`, return a `State[CalcState, Unit]`.

For example:
```scala
private def calc(i: Int)(cs: CalcState): CalcState = ???
```
becomes
```scala
private def calc(i: Int): State[CalcState, Unit] = ???
```
The function internals can then be wrapped in a `State.modify`.

 - Rewrite `calc(i: Int`, `write` and `equals` to use the state monad.
 

### Stacking monads

The signature for `calc(o: BinOp)` can't easily be refactored:
```scala
private def calc(o: BinOp)(cs: CalcState): ConsecutiveOpError Either CalcState = ???
```
This requires both a failure aspect and a stateful aspect.  To use it properly, we need to somehow combine both the `State` monad and `Either` monad together.

### A State - Either combo

Let's consider a more complicated example of `fooBar`:
``` scala
def foo(cs: CalcState): CalcError Either CalcState = ???
def bar(cs: CalcState): CalcError Either (CalcState, Int) = ???
def baz(i: Int)(cs: CalcState): CalcError Either CalcState = ???

def together(cs: CalcState): CalcError Either CalcState = { 
  val es0 = foo(cs)
  val es1 = es0.flatMap(s0 => bar(s0))
  es1.flatMap {
    case (s1, i) => baz(i)(s1)
  }
}
```

We want to pipe the output of `foo` to `bar` to `baz` as before, but now each function returns an `Either` of a failure or a successfully altered state.
Let's create a `StateTransformer[S, A]` which represents `S => CalcError Either (S, A)`.  It should have a `pipe` like the `StateFunction`, this time taking in a `A => StateTransformer[S, B]`.

```scala
case class StateTransformer[S, A](f: S => CalcError Either (S, A)) {
  def pipe[B](g: A => StateTransformer[S, B]): StateTransformer[S, B] = new StateTransformer[S, B]({ cs =>
    f(cs).flatMap {
      case (s0, a) => g(a).f(s0)
    }
  })
}
```

The `fooBar` example then becomes:
``` scala
def foo: StateTransformer[CalcState, Unit] = ???
def bar: StateTransformer[CalcState, Int] = ???
def baz(i: Int): StateTransformer[CalcState, Unit] = ???

val together: StateTransformer[CalcState, Unit] = 
  foo.pipe(_ => bar).pipe(baz)
```

It's interesting to note that if `foo` fails and returns a `Left`, `bar` and `baz` do not get executed.

### More parametricity
Note that in the `pipe` method of the `StateTransformer`, we aren't using any properties of `Either`, other than the fact that it has a `flatMap`.
This means that we can parameterize the `StateTransformer` on any datatype `F[_]`, provided that it has a `Monad` instance:

``` scala
class StateTransformer[F[_], S, A](f: S => F[(S, A)]) {
  def pipe[B](g: A => StateTransformer[F, S, B])(implicit M: Monad[F]): StateTransformer[F, S, B] = 
    new StateTransformer[F, S, B] { cs =>
      f(cs).flatMap {
       case (s0, a) => g(a).f(s0)
      }
    }
}
```

### The StateT
In fact, the `StateTransformer` already exists in cats, under the name of `StateT[F[_], S, A]`.  The cats `State` is actually a type alias to `StateT`, where `F[_]` is `Id`.  The `T` suffix is short for *transformer*, because we take any monadic datatype `F` and transform it to give it statelike properties.  There are other transformers too.  `EitherT` gives any monadic `F[_]` the properties of the `Either` datatype.

### Stacking monads high
Each feature of a functional program has its own monadic datatype, and these are stacked using transformers.  A complete program then has a monad stack associated with it, on which all functions operate.

In our case, we stack a `State` and an `Either` using a `StateT`:
```scala
type EitherS[A] = CalcError Either A
type MStack[A] = StateT[EitherS, CalcState, A]`
```
This can be written more cleanly using [kind-projector](https://github.com/non/kind-projector):
```scala
type MStack[A] = StateT[CalcError Either ?, CalcState, A]
```

Add kind projector as a compiler plugin in your `build.sbt`:

```scala
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")
```

Add the type alias `MStack` to `FunCalculator`.

The function `calc(o: BinOp)` then has the signature:
```scala
def calc(o: BinOp): MStack[Unit] = ...
```

`parse` still returns an `Either`, so we need to be able to **lift** `Either` instances into the monad stack.

Create the function `liftEither`:
```scala
private def liftEither[E <: CalcError, A](e: E Either A): MStack[A] = 
    StateT.lift(e.leftWiden[CalcError])
```

`State` instances also need to be lifted into the stack.  Create the function `liftState`:
```scala
private def liftState[A](s: State[CalcState, A]): MStack[A] = 
    s.transformF(a => Right(a.value))
```

Rewrite `press` to use `liftEither` and `liftState` to lift instances into the monad stack.

The `FriendlyCalculator` can then run the stack produced by `press` to get the next state.

### Summary

In this step, we hope you've learned:

 1. How to divide a program into features, such as stateful computation and error handling
 2. To represent each feature with a monadic datatype
 3. How to stack monadic datatypes using transformers
 4. How to lift datatypes into the stack
 
### Next Steps

Checkout step 3 with:
```bash
git checkout -f step-3-mtl
```
