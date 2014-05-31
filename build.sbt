lazy val core = Project("frolic-core", file("core"))

lazy val netty = Project("frolic-netty", file("netty")).dependsOn(core)

lazy val helloWorldDemo = Project("demo-helloworld", file("demo/helloworld")).dependsOn(core, netty)

lazy val nettyDemo = Project("demo-netty", file("demo/netty")).dependsOn(core, netty)

