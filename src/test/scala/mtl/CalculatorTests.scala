package mtl

import org.scalatest._

trait CalculatorTests extends FunSpec with Matchers {

  def calculator: Calculator

  it("displays 5 after 3+2=") {
    val c = calculator
    val r = c.press('3')
      .press('+')
      .press('2')
      .press('=').screen
    r shouldBe "5"
  }

  it("displays an empty string on start") {
    val c = calculator
    c.screen shouldBe ""
  }

  it("displays the result of a calculation on typing =") {
    val c = calculator
    val r = c.press('2').press('+').screen
    r shouldBe "2+"
  }

  it("displays 35 after 30+5=") {
    val c = calculator
    val r = c.press('3')
      .press('0')
      .press('+')
      .press('5')
      .press('=').screen
    r shouldBe "35"
  }

  it("displays 35 after 5+30=") {
    val c = calculator
    val r = c.press('5')
      .press('+')
      .press('3')
      .press('0')
      .press('=').screen
    r shouldBe "35"
  }

  it("displays -2 after -2=") {
    val c = calculator
    val r = c.press('-').press('2').press('=').screen
    r shouldBe "-2"
  }

  it("displays ERROR on typing a non-symbolic character") {
    val c = calculator
    val r = c.press('a').screen
    r shouldBe "ERROR"
  }

  it("displays ERROR on typing two operators twice") {
    val c = calculator
    val r = c.press('2').press('+').press('+').screen
    r shouldBe "ERROR"
  }
}

class EvilCalculatorTests extends CalculatorTests {
  def calculator: Calculator = new EvilCalculator()
}

class FriendlyCalculatorTests extends CalculatorTests {
  def calculator: Calculator = new FriendlyCalculator()
}
