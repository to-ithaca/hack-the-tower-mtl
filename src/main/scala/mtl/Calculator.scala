package mtl

trait Calculator {
  def press(c: Char): Calculator
  def screen: String
}
