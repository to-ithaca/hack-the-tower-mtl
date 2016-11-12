# MTL workshop

## Step 3 - Monad Transformer Library
You're almost done!

The next section describes how to take functions defined on a concrete monad stack and parameterize them using MTL style programming.  MTL is a really cool application of parametricity, and is just an extension of everything we've been doing up to this point.

### Watch this space
The MTL style in cats is still in development, and is likely to have several enhancements over the coming months.  It's likely that the finished style will be a bit different to the one described here.  

### Stacked too high
Our program now has a single high level monad stack, aliased `MStack`, which encompases both `State` and `Either` like behaviour.  If this was a commercial application, this would be a small part of a much larger stack, which would most likely have other aspects.  We may want to read a calculator format from a `Config` using a `Reader` monad, or collect logs with a `Writer` monad.  Our final stack may be very large, and we would have to take a lot of effort lifting functions into the stack.

Our functions shouldn't actually care about the stack we use for our wider application.  They only need to know that that stack has `State` and `Either` like behaviour.  If we could somehow parameterize our functions on our stack, we wouldn't have to lift them in, or reference a concrete stack at all.  

*This is what Monad Transformer Library is for*.

MTL allows us to parameterize functions on monad stacks.  Provided a stack has the required behaviour, we can use it in a function.

Let's take the equals function as a simple example:
```scala
def equals: State[CalcState, Unit] = ???
```

`equals` currently returns `State`, but we can make it return an arbitrary stack `F[_]` provided that `F` has stateful properties:
```scala
def equals[F[_]](implicit M: MonadState[F, CalcState]): F[Unit] = ???
```

If we call `equals[State[CalcState, ?]]`, we get a `State` back.
We could also call `equals[MStack]` and get an `MStack` back.

### The MonadState
We're going to rewrite `calc(i: Int)`, `equals` and `write` using the `MonadState`.  Take a look at the cats code - you'll notice that the `MonadState` is actually pretty simple.
It has a few base methods:

 - `get`
 - `set`

From these we can derive `inspect` and `modify`.
It inherits a few methods from `Monad`:

 - `pure`
 - `flatMap`
 - `map`

Let's write `calc(i: Int)` in terms of these:
```scala
private def calc[F[_]](i: Int)(implicit M: MonadState[F, CalcState]): F[Unit] = M.modify(cs =>
    cs.copy(expr = cs.expr match {
      case Num(c) => Num(c * 10 + i)
      case NumOp(p, o) => NumOpNum(p, o, i)
      case NumOpNum(p, o, c) => NumOpNum(p, o, c * 10 + i)
}))
```
It's pretty much the same as before, but we use `M.modify` instead of `State.modify`.

Write `write` in terms of the `MonadState`:
```scala
private def write[F[_]](s: String)(implicit M: MonadState[F, CalcState]): F[Unit] = 
  M.modify(cs => cs.copy(display = cs.display + s))
```

Write `equals` in terms of the `MonadState`:
```scala
  private def equals[F[_]](implicit M: MonadState[F, CalcState]): F[Unit] = M.modify { cs =>
    val value = cs.expr match {
      case Num(i) => i
      case NumOp(p, o) => binop(p, o, 0)
      case NumOpNum(p, o, n) => binop(p, o, n)
    }
    CalcState(Num(value), value.show)
}
```

### The MonadError
The `MonadError` is an MTL typeclass representing failure handling.  It's also a fairly simple typeclass.

 - It has a method `raiseError` for raising errors into the stack.
 - It inherits `pure`, `flatMap` and `map` from `Monad`.

Write `parse` in terms of the `MonadError`:
```scala
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
```
### Stacking MTL
We now need to parameterize `calc(o: BinOp)` on a monad stack.  The signature itself is easy to craft.
We need to have both a `MonadState` and a `MonadError` typeclass.
```scala
 private def calc[F[_]](o: BinOp)(implicit ME: MonadError[F, CalcError],
                                           MS: MonadState[F, CalcState]): F[Unit] = ???
```
Let's try:
```scala
 private def calc[F[_]](o: BinOp)(implicit ME: MonadError[F, CalcError],
                                            MS: MonadState[F, CalcState]): F[Unit] =
   MS.get.flatMap(cs =>
    cs.expr match {
      case Num(n) =>  MS.set(cs.copy(expr = NumOp(n, o)))
      case NumOp(n, p) => ME.raiseError(ConsecutiveOpError(p, o))
      case NumOpNum(p, po, n) => MS.set(cs.copy(expr = NumOp(binop(p, po, n), o)))
    })
```
Unfortunately, this has **ambiguous implicits** for `flatMap` - both the `MonadError` and `MonadState` provide implicit conversions for `flatMap`, and the scala compiler doesn't know which one to use.  We have to pick one by calling `flatMap` explicity from a typeclass:
```scala
private def calc[F[_]](o: BinOp)(implicit ME: MonadError[F, CalcError], 
                                            MS: MonadState[F, CalcState]): F[Unit] =
   MS.flatMap(MS.get) ( cs =>
    cs.expr match {
      case Num(n) =>  MS.set(cs.copy(expr = NumOp(n, o)))
      case NumOp(n, p) => ME.raiseError(ConsecutiveOpError(p, o))
      case NumOpNum(p, po, n) => MS.set(cs.copy(expr = NumOp(binop(p, po, n), o)))
    })
```
This isn't too bad for our code, but for large, nested flatMaps, it can get very ugly.  This is an issue with the way typeclasses are coded in cats, and there is much discussion on the solution.  Until then, explicit `flatMap` calls are the best way to go.

Let's finally write `press` on a concrete stack by passing `MStack` as a type parameter to our functions:
```scala
 def press(c: Char): MStack[Unit] = for {
    s <- parse[MStack](c)
    _ <- s match {
      case Number(i) => calc[MStack](i) >> write[MStack](i.show)
      case o: BinOp =>  calc[MStack](o) >> write[MStack](o.show)
      case Equals =>  equals[MStack]
    }
} yield ()
```

And voila! We finally have a functional MTL-style calculator!

### Summary
In this step you should have:

 1. Learned how to parameterise features, such as state and error handling, using MTL
 2. Learned how to use mutliple MTL typeclasses together for combinations of effects

### And Finally
Checkout step 4 to take a look at the finished code:

```bash
git checkout -f step-4-extras
```
