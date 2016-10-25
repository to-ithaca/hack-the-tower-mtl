import cats._
import cats.data._
import cats.implicits._

package object mtl {
  implicit def StateTMonadError[F[_], S, E](implicit ME: MonadError[F, E]): MonadError[StateT[F, S, ?], E] =
    new StateTMonadError[F, S, E] {
      def M = ME
    }
}



trait StateTMonadError[F[_], S, E] extends MonadError[StateT[F, S, ?], E] {
  implicit def M: MonadError[F, E] 

  def pure[A](x: A): StateT[F,S,A] = StateT.pure(x)
  def handleErrorWith[A](fa: StateT[F,S,A])(f: E => StateT[F,S,A]): StateT[F,S,A] = StateT { s => 
    M.handleErrorWith(fa.run(s))(e => f(e).run(s))
  }
  def raiseError[A](e: E): StateT[F,S,A] = StateT.lift(M.raiseError(e))
  
  def flatMap[A, B](fa: StateT[F,S,A])(f: A => StateT[F,S,B]): StateT[F,S,B] = fa.flatMap(f)
  def tailRecM[A, B](a: A)(f: A => StateT[F,S,Either[A,B]]): StateT[F,S,B] = 
    Monad[StateT[F, S, ?]].tailRecM(a)(f)
}
