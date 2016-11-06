package akka.persistence.titan.common

/**
  * A dumb message - the idea is to have something more complex than a single string
  *
  * @param payload
  * @param additionalInfo
  * @param aNumber - 42 obviously
  * @param aNestedCaseClass
  */
case class ComplexMessage(payload: String,
                          additionalInfo: String = "some additional info",
                          aNumber: Integer = 42,
                          aNestedCaseClass: ComplexBody = ComplexBody())
