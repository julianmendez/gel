#  [Generalizations for the EL family](https://julianmendez.github.io/gel)

[![build](https://github.com/julianmendez/gel/workflows/Java%20CI/badge.svg)](https://github.com/julianmendez/gel/actions)
[![maven central](https://maven-badges.herokuapp.com/maven-central/de.tu-dresden.inf.lat.gel/gel/badge.svg)](https://search.maven.org/#search|ga|1|g%3A%22de.tu-dresden.inf.lat.gel%22)
[![license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)

This project implements generalization algorithms (role-depth bounded least common subsumer and most specific concept) for the description logics EL and extensions thereof.


## Download

* [Protégé plug-in](https://sourceforge.net/projects/latitude/files/gel/0.17.2/de.tu-dresden.inf.lat.gel-0.17.2.jar/download)
* [The Central Repository](https://repo1.maven.org/maven2/de/tu-dresden/inf/lat/gel/)
* as dependency

```xml
<dependency>
  <groupId>de.tu-dresden.inf.lat.gel</groupId>
  <artifactId>gel</artifactId>
  <version>0.17.2</version>
</dependency>
```


## Developers

Original Developer: [Andreas Ecke](https://lat.inf.tu-dresden.de/~ecke)

Additional Developer: [Julian Mendez](https://julianmendez.github.io)


## Source code

To checkout and compile the project, use:

```
$ git clone https://github.com/julianmendez/gel.git
$ cd gel
$ mvn clean install
```

To compile the project offline, first download the dependencies:

```
$ mvn dependency:go-offline
```

and once offline, use:

```
$ mvn --offline clean install
```

The bundles uploaded to [Sonatype](https://oss.sonatype.org/) are created with:

```
$ mvn clean install -DperformRelease=true
```

and then:

```
$ cd gel/target
$ jar -cf bundle.jar gel-*
```

The version number is updated with:

```
$ mvn versions:set -DnewVersion=NEW_VERSION
```

where *NEW_VERSION* is the new version.


## License

This software is distributed under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt).


## Release Notes

See [release notes](https://julianmendez.github.io/gel/RELEASE-NOTES.html).


## Description

The GEL plugin for [Protégé](https://protege.stanford.edu/) implements the **least common subsumer** and **most specific concept** inferences for EL-based ontologies in Protégé. The current version of these generalizations works with ELH ontologies and uses [jcel](https://github.com/julianmendez/jcel) as underlying classifier.


## Installation

To install, just download the jar-file (`de.tu-dresden.inf.lat.gel-`*version*`.jar`) and move it to the plugin folder of the Protégé directory. After restarting Protégé, you should find "Least Common Subsumer" and "Most Specific Concept" in the menu "Window > Views > Ontology Views". Just drag these somewhere onto the window and you should be ready to get started.


## Use

When you drop the "Least Common Subsumer" (lcs) view somewhere, you will get various options for the lcs. You can compute the least common subsumer of up to 8 concept descriptions. In the top of the lcs-view, you see these eight concepts, which are initialized with the top-concept "Thing". To change them, click on the edit-button on the right side. A window pops up, where you can enter an arbitrary EL-concept description. Clicking "OK" will change the corresponding field. On the left side of each of the eight concepts, you will see checkboxes. Only these concepts where the checkbox is checked will be used for the computation of the lcs. In the bottom part of the view, you can choose a maximum role-depth, which bounds the lcs, and wether the result should be simplified. A click on "Compute Lcs" will run the computation and the resulting concept description will pop up in a new window.

For the "Most Specific Concept" (msc), you have the same options (maximum role-depth and whether or not you want to simplify the result), but instead of several concept descriptions you can only choose a single individual in the top part of the view. A click on "Compute Msc" will run the computation of the most specific concept for that individual and the resulting concept description will pop up in a new window.


## Contact

In case you need more information, please contact @julianmendez .


