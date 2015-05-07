name := "appmgr"

mainClass := Some("app.Main")

target in App := target.value / "appmgr" / "root"
