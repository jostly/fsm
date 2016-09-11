import sbtrelease._
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion=>_,_}

lazy val commonSettings = Seq(
  organization := "com.github.jostly",
  scalaVersion := "2.11.8",

  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-Xlint:missing-interpolator",
    "-Ywarn-unused-import",
    "-Ywarn-unused",
    "-Ywarn-dead-code",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-explaintypes"
  )
)


lazy val fsm = (project in file(".")).
  enablePlugins(GitVersioning, GitBranchPrompt).
  aggregate(core).
  dependsOn(core).
  settings(commonSettings ++ publishSettings:_*)

lazy val core = project.
  enablePlugins(GitVersioning, GitBranchPrompt).
  settings(commonSettings ++ publishSettings:_*)


lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/jostly/fsm")),
  startYear := Some(2016),
  licenses := Seq(("Unlicense", url("http://unlicense.org"))),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/jostly/fsm"),
      "scm:git:https://github.com/jostly/fsm.git",
      Some("scm:git:git@github.com:jostly/fsm.git")
    )
  ),
  bintrayVcsUrl := Some("scm:git:git@github.com:jostly/fsm.git"),
  bintrayCredentialsFile := file(".credentials"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := <developers>
    <developer>
      <id>jostly</id>
      <name>Johan Östling</name>
      <url>https://github.com/jostly</url>
    </developer>
  </developers>
)

git.useGitDescribe := true
git.baseVersion := "0.0.1"

// sbt release
def setVersionOnly(selectVersion: Versions => String): ReleaseStep =  { st: State =>
  val vs = st.get(ReleaseKeys.versions).getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?"))
  val selected = selectVersion(vs)

  st.log.info("Setting version to '%s'." format selected)
  val useGlobal =Project.extract(st).get(releaseUseGlobalVersion)
  val versionStr = (if (useGlobal) globalVersionString else versionString) format selected

  reapply(Seq(
    if (useGlobal) version in ThisBuild := selected
    else version := selected
  ), st)
}

lazy val setReleaseVersion: ReleaseStep = setVersionOnly(_._1)

releaseVersion <<= (releaseVersionBump)( bumper=>{
  ver => Version(ver)
    .map(_.withoutQualifier)
    .map(_.bump(bumper).string).getOrElse(versionFormatError)
})

val showNextVersion = settingKey[String]("the future version once releaseNextVersion has been applied to it")
val showReleaseVersion = settingKey[String]("the future version once releaseNextVersion has been applied to it")
showReleaseVersion <<= (version, releaseVersion)((v,f)=>f(v))
showNextVersion <<= (version, releaseNextVersion)((v,f)=>f(v))

releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  runTest,
  tagRelease,
  pushChanges
)
