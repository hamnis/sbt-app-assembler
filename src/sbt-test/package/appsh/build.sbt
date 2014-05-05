name := "appsh"

mainClass := Some("app.Main")

appAssemblerSettings

appOutput in App := target.value / "appsh" / "root"
