package appassembler

import sbt._
import Keys._
import Load.BuildStructure
import Def.Initialize

import java.io.File

object AppAssemblerPlugin extends AutoPlugin {

  val appConfig = TaskKey[AppConfig]("Configuration, internally used")
  val appDependencies = TaskKey[Seq[(Def.Classpath, ProjectRef)]]("Dependencies")
  val appClasspath = TaskKey[Seq[(Def.Classpath, ProjectRef)]]("Classpath")
  val appProjectJars = TaskKey[Seq[(Jar, ProjectRef)]]("Project artifacts")

  object autoImport {
    val App = config("app") extend(Runtime)

    val appAutoIncludeDirs = TaskKey[Seq[Directory]]("Files are copied from these directories")
    val appJvmOptions = SettingKey[Seq[String]]("JVM parameters to use in start script")
    val appPrograms = TaskKey[Seq[Program]]("Configured programs. Will generate scripts for jvm programs")
    val appExclude = SettingKey[Seq[String]]("exclude these artifact files")
  }

  import autoImport._

  override def trigger = allRequirements

  override def requires = plugins.JvmPlugin

  override def projectSettings = inConfig(App)(Seq(
      packageBin <<= distTask,
      Keys.`package` <<= distTask,
      clean <<= distCleanTask,
      unmanagedResourceDirectories <<= (unmanagedResourceDirectories in Runtime),
      target := (target in Compile).value / "appassembler",
      appAutoIncludeDirs <<= defaultAutoIncludeDirs,
      appJvmOptions := Nil,
      appPrograms <<= (appPrograms in Compile, discoveredMainClasses in Compile, streams) map { (pgs, classes, str) =>
        val programs = if (!pgs.isEmpty) pgs else classes.map(mc => Program(mc))
        if (programs.isEmpty) {
          str.log.warn("No Main classes detected.")
        }
        programs
      },
      appExclude := Seq.empty[String],
      appClasspath <<= (thisProjectRef, buildStructure, appExclude) flatMap getFromSelectedProjects(fullClasspath in Runtime),
      appDependencies <<= (thisProjectRef, buildStructure, appExclude) flatMap getFromSelectedProjects(externalDependencyClasspath in Runtime),
      appProjectJars <<= (thisProjectRef, buildStructure, appExclude) flatMap getFromSelectedProjects(packageBin in Runtime),      
      appConfig <<= (target, appAutoIncludeDirs, appJvmOptions, appPrograms) map AppConfig)) ++
      Seq(appPrograms in Compile := Nil)

  private def distTask = (appConfig, appProjectJars, appClasspath, appDependencies, streams) map {
      (conf, projectJars, classpath, deps, streams) ⇒
        import sbt.classpath.ClasspathUtilities

        val libs         = classpath.flatMap(_._1.map(_.data)).toVector.filter(ClasspathUtilities.isArchive)
        val depLibs      = deps.flatMap(_._1.map(_.data)).toSet.filter(ClasspathUtilities.isArchive)
        val libsFiltered = (libs flatMap {
          case jar if depLibs contains jar.asFile => Some(jar)
          case jar => None
        }) ++ projectJars.map(_._1)

        streams.log.info("Creating distribution %s ...".format(conf.output))
        try {

          val file = DistBuilder.create(conf, libsFiltered)(streams.log)
          streams.log.info("Distribution created in %s.".format(file))
          file
        } catch {
          case e : Exception => {e.printStackTrace(); sys.error(e.getMessage)}
        }
    }

  private def distCleanTask = (target in App, streams) map {
      (output, s) ⇒        
          s.log.info("Cleaning " + output)
          IO.delete(output)
  }

  private def defaultAutoIncludeDirs = (sourceDirectory, target) map { (src, target) => 
    Seq(src / "app", target / "app")
  }

  private def getFromSelectedProjects[T](targetTask: TaskKey[T])(currentProject: ProjectRef, structure: sbt.BuildStructure, exclude: Seq[String]): Task[Seq[(T, ProjectRef)]] = {
    def allProjectRefs(currentProject: ProjectRef): Seq[ProjectRef] = {
      def isExcluded(p: ProjectRef) = exclude.contains(p.project)
      val children = Project.getProject(currentProject, structure).toSeq.flatMap {
        p =>
          p.uses
      }

      (currentProject +: (children flatMap (allProjectRefs(_)))) filterNot (isExcluded)
    }

    val projects: Seq[ProjectRef] = allProjectRefs(currentProject).distinct
    projects.map(p => (Def.task {((targetTask in p).value, p)}) evaluate structure.data).join
  }
}

