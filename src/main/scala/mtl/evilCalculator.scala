package mtl

import scala.collection.mutable.StringBuilder
object Operator extends Enumeration {
  val Plus, Minus = Value
}


final class EvilCalculator extends Calculator { self =>

  private var _previous: Option[Int] = None
  private var _currentOperator: Operator.Value = null
  private var _current: Option[Int] = None
  private var _screen: StringBuilder = new StringBuilder

  def screen: String = {
    _screen.toString()
  }

  def press(e: Char): Calculator = {
    if(screen == "ERROR") {
      clear()
    }
    try {
      val s = e.toString
      if(s == "+") {
        plus()
      } else if(s == "-") {
        minus()
      } else if(s == "=") {
        equals()
      } else {
        press(Integer.parseInt(s))
      }
    } catch {
      case _: Throwable => error()
    }
  }
  
  private def noOperator() = {
    _currentOperator = null
  }

  private def setCurrent(n: Int): Unit = {
    _current = Some(n)
  }

  private def resetCurrent(): Unit = {
    _current = None
  }

  private def resetCalc(): Unit = {
    noOperator()
    resetCurrent()
    resetCurrent()
  }

  private def setPrevious(n: Int): Unit = {
    _previous = Some(n)
  }

  private def resetPrevious(): Unit = {
    _previous = None
  }

  private def appendScreen(n: Int): Unit = {
    _screen.append(n)
  }

  private def appendScreen(s: String): Unit = {
    _screen.append(s)
  }

  private def press(n: Int): Calculator = {
    if(_current.nonEmpty) {
      setCurrent(_current.get * 10 + n)
    } else {
      setCurrent(n)
    }
    appendScreen(n)
    self
  }

  private def calculate(): Unit = {
    if(_previous.nonEmpty && _current.nonEmpty && _currentOperator != null) {
      if(_currentOperator == Operator.Plus) {
        setPrevious(_previous.get + _current.get)
        resetCurrent()
      } else if(_currentOperator == Operator.Minus) {
        setPrevious(_previous.get - _current.get)
        resetCurrent()
      }
    } else if(_previous.isEmpty && _current.nonEmpty && _currentOperator == null) {
      setPrevious(_current.get)
      resetCurrent()
    } else if(_previous.isEmpty && _current.isEmpty && _currentOperator == null) {
      setPrevious(0)
      resetCurrent()
    } else if(_current.isEmpty && _currentOperator != null) {
      throw new IllegalArgumentException("Sequential operators typed")
    }
  }

  private def plus(): Calculator = {
    appendScreen("+")
    calculate()
    _currentOperator = Operator.Plus
    self
  }

  private def minus(): Calculator = {
    appendScreen("-")
    calculate()
    _currentOperator = Operator.Minus
    self
  }

  private def equals(): Calculator = {
    calculate()
    _screen = new StringBuilder(_previous.get.toString)
    noOperator()
    self
  }

  private def clear(): Calculator = {
    _screen = new StringBuilder()
    resetCalc()
    self
  }

  private def error(): Calculator = {
    clear()
    appendScreen("ERROR")
    self
  }

}
