lazy val core = project

lazy val demo1 = project.dependsOn(core)