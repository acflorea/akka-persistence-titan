package akka.persistence.titan.common

/**
  * A dumb message body
  *
  * @param aStringFiled
  * @param aDoubleFiled - can only by 42.42
  */
case class ComplexBody(aStringFiled: String = "some random value", aDoubleFiled: Double = 42.42)
