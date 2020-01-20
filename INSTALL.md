# Installing CSW

## Prerequisites

* The CSW software is build using [sbt](https://www.scala-sbt.org). 
Be sure to install the latest version and add it to your shell path.

* This version of CSW also requires [Java-11](https://openjdk.java.net/projects/jdk/11/).
On Linux systems java can be installed using the standard tools (`yum` on CentOS, `dnf` on Fedora, `apt` on Ubuntu, etc.).
On Macs, you can use [AdoptOpenJDK](https://github.com/AdoptOpenJDK/homebrew-openjdk),
which is also available for [Linux](https://adoptopenjdk.net/installation.html?variant=openjdk11&jvmVariant=hotspot#x64_linux-jdk).

## Building the Software

To compile and install CSW locally, run:

    sbt publishLocal stage

This installs the software under `target/universal/stage`.

## Installing the documentation

The CSW [paradox](https://developer.lightbend.com/docs/paradox/current/index.html) documentation can be installed with:

    sbt clean makeSite

This puts the documentation under `target/site` with the entry point at `target/site/csw/0.1.0-SNAPSHOT/index.html`.

For more details on the sbt tasks, see the [online documentation](https://tmtsoftware.github.io/csw/1.0.0/commons/sbt-tasks.html).

For information about making a CSW release, see [here](RELEASING.md).


