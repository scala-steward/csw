# Deploying Components

## ContainerCmd

`ContainerCmd` is a helper utility provided as a part of the framework. This helps component writers to start their components inside a container,
but can also be used to start standalone components (not in a container).

A main application needs to be created which uses the framework provided utility `csw.framework.deploy.containercmd.ContainerCmd` 
to start a container or standalone component. The utility supports the following parameters, which can be provided as arguments to the
application :

* fully qualified path of the configuration file
* **local** if the above path is a path to a file available on local disk. If this argument is not provided the file will be looked
up in the Configuration Service using the same path.
* **standalone** if the configuration file describes a component to be run in standalone mode. If this argument is not provided the 
application expects a configuration file describing a container component and will use it to start a container with all the
components as described in the file.

Scala
:   @@snip [ContainerCmdApp.scala](../../../../examples/src/main/scala/example/framework/ContainerCmdApp.scala) { #container-app }

Java
:   @@snip [JContainerCmdApp](../../../../examples/src/main/java/example/framework/JContainerCmdApp.java) { #container-app }

@@@ note

It is not necessary to have name of the application as ContainerCmdApp/JContainerCmdApp; the user can choose any name.

@@@

Starting a **standalone** component from a **local** configuration file

    `./container-cmd-app --standalone --local /assembly/config/assembly.conf`
    
Starting a **container** component from a configuration file available in **configuration service**

    `./container-cmd-app /assembly/config/assembly.conf`

## Container for deployment

A container is a component which starts one or more components and keeps track of the components within a single JVM process. When started, the container also registers itself with the Location Service.
The components to be hosted by the container are defined using a `ContainerInfo` model which has a set of ComponentInfo objects. It is usually described in a configuration file but can also be created programmatically.

SampleContainerInfo
:   @@@vars
    ```
    name = "Sample_Container"
    components: [
      {
        componentType = assembly
        behaviorFactoryClassName = package.component.SampleAssembly
        prefix = wfos.SampleAssembly
        locationServiceUsage = RegisterAndTrackServices
        connections = [
          {
            prefix: wfos.Sample_Hcd_1
            componentType: hcd
            connectionType: akka
          },
          {
            prefix: wfos.Sample_Hcd_2
            componentType: hcd
            connectionType: akka
          },
          {
            prefix: wfos.Sample_Hcd_3
            componentType: hcd
            connectionType: akka
          }
        ]
      },
      {
        prefix = "wfos.Sample_Hcd_1"
        componentType = hcd
        behaviorFactoryClassName = package.component.SampleHcd
        prefix = abc.sample.prefix
        locationServiceUsage = RegisterOnly
      },
      {
        prefix = "wfos.Sample_Hcd_2"
        componentType: hcd
        behaviorFactoryClassName: package.component.SampleHcd
        prefix: abc.sample.prefix
        locationServiceUsage = RegisterOnly
      }
    ]
    ```
    @@@
    
## Standalone components

A component can be run alone in a standalone mode without sharing its JVM space with any other component. 

Sample Info for an assembly
:   @@@vars
    ```
    componentType = assembly
    behaviorFactoryClassName = csw.common.components.command.ComponentBehaviorFactoryForCommand
    prefix = tcs.mobie.blue.monitor.Monitor_Assembly
    locationServiceUsage = RegisterOnly
    ```
    @@@