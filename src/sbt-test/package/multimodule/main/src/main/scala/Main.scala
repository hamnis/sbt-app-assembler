package app

object DepImpl extends Dep {
  val value = "Value is here"
}


object Main extends App {
  println(DepImpl.value)
}
