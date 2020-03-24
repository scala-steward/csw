# Getting Started

In this tutorial, you’ll see how to create a Scala/Java project using a [giter8](http://www.foundweekends.org/giter8/) template for CSW ([csw.g8](https://github.com/tmtsoftware/csw.g8)) which contains sample handlers for creating HCD and Assembly. 
It also contains a deploy project which is responsible for starting multiple components or containers. You can use this as a starting point for your own projects for writing component. 
We’ll use the [sbt](https://www.scala-sbt.org/1.x/docs/index.html) build tool which compiles, runs, and tests your projects among other related tasks.

## Installation
Supported Operating Systems are: CentOS and MacOS
 
1.  Make sure you have the Java AdoptOpenJDK 11.
    -   Run  `javac -version`  in the command line and make sure you see  `javac 11._._`
    -   If you don’t have version 11 or higher, links given below will help you to reach there.
        - [Mac](https://github.com/AdoptOpenJDK/homebrew-openjdk)
        - [Linux](https://adoptopenjdk.net/installation.html?variant=openjdk11&jvmVariant=hotspot)                
2.  Install sbt
    -   [Mac](https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Mac.html)
    -   [Linux](https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html)
3. Install IntelliJ 
	- [MAC](https://www.jetbrains.com/idea/download/)
	- [Linux](https://www.jetbrains.com/idea/download/)
4. Install following IntelliJ Plugins
    - [Scala](https://plugins.jetbrains.com/plugin/1347-scala)
    - [Scalafmt](https://plugins.jetbrains.com/plugin/8236-scalafmt)
5. Install [Redis](https://redis.io/download) - Version should be greater than 4.0.0
6. Recommended testing frameworks/tools: 
	- [ScalaTest](http://www.scalatest.org/)
	- [JUnit](https://junit.org/junit4/), JUnit Interface
	- Note: These frameworks are typically downloaded and made available by the sbt tool by specifying them as dependencies.
	If you are using the [giter8](https://github.com/tmtsoftware/csw.g8) template (see below), these dependencies are specified by default, and the sbt
	will resolve them when it runs.  


## Create project

1.  `cd`  to an empty folder.
2.  Run the following command  `sbt new tmtsoftware/csw.g8`. This pulls the ‘csw’ template from GitHub.
3.  Provide input details when prompted. Follow the template [readme.md](https://github.com/tmtsoftware/csw.g8/blob/master/README.md) for detailed information about input parameters.


To open the project in IntelliJ, start IntelliJ and click on Import Project in the Intro dialog.  If you have a project already open, click on File -> New -> Project from Existing Sources...   

![intellijIntro](../images/gettingstarted/intellijIntro.png)

Then select the directory created by the template and click Open.

You will then see a dialog asking how to import the project.  

![intellijImport](../images/gettingstarted/intellijImport.png)

On the next dialog, click the Project JDK dropdown and select Java 11.

![intellijJava](../images/gettingstarted/intellijJava.png)

Then click Finish to create the project.  Once IntelliJ loads with the new project, you may have to import changes
to properly load it.

Let’s take a look at what just got generated:

In this example, a project was created with default parameters.  Below, we show some pictures and outline the project 
structure.  Much of what is described below is described in more detail on the subsequent pages.

As you can see in below snapshot, template will create three projects:

    - `sample`
    - `samplehcd`
    - `sample-deploy`
    
![sample-project-structure](../images/gettingstarted/sampleProjectLayout.png)

1. This shows where the module for the assembly is located, called `sample`

2. The template generates handlers and factories for both Java and Scala.  Both are not required to develop a 
component.  After you choose which language you want to develop in, you can delete the other.  We encourage you
to use Scala!  It has good support for asynchronous programming. 

3. This is the factory for creating your component actor behavior, which is created by the framework.  This does
not need to be modified.

4. This is the handlers class where you implement your custom logic.

5. The template creates an example test fixture for testing your code in both Java and Scala.

6. Sample component configuration files are created to facilitate testing.  The actual component configuration files
needed for running your component are located in the deploy module (see below). 

7. The HCD code is in a separate module here.

The following diagram shows the deploy folder and the project folder:

![sample-deploy](../images/gettingstarted/sampleDeploy.png)

1.  `sample-deploy` project is used to create a concrete implementation.  This allows for the construction of a complete binary
package bundled with all dependencies, and a launching application.  This part shows the configuration files used in
component or container construction.

2. These are the applications used for component and container construction.  These don't need to be modified.

3. This folder contains the project build settings, along with the build.sbt file.  You shouldn't have to modify anything
here other than your dependencies (see below).
 
4. If you need to add a library or other dependency to you project, the Dependencies and Libs files may need to be updated.
Libs contains a list of all external libraries, and Dependencies indicate which libraries are used by each module.  The
template comes with `csw` and other useful library dependencies already included. 

![sample-project](../images/gettingstarted/sampleProjectFolder.png)

## Add new sbt project module

If you want to add another component to the project, for example, with the name `sample-io`, you have to create a new sbt module:

1. Add external library dependencies required by `sample-io` in `Libs.scala` file, if it does not exist.
```
val `akka-actor` = "com.typesafe.akka" %% "akka-actor" % "2.6.1"
```
2. Map new/existing library dependencies in `Dependencies.scala` file against new project.
```
val SampleIO = Seq( Libs.`akka-actor` )
```
3. Include below snippet in `build.sbt` file, this will create new sbt project module.
```
lazy val `sample-io` = project
  .settings( libraryDependencies ++= Dependencies.SampleIO )
``` 
4. If your new module depends on code from other modules within this project, use `.dependsOn` in your build.sbt file:
``` 
lazy val `sample-io` = project
  .settings( libraryDependencies ++= Dependencies.SampleIO )
  .dependsOn(
    `sample`,
    `samplehcd`
  )
```
5. Update the deployment dependencies:
``` 
lazy val `sample-deploy` = project
  .dependsOn(
    `sample`,
    `samplehcd`,
    `sample-io`
  )
```
