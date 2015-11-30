
## Release Notes

| version | release date | Java | OWL API       | Protégé       |
|:--------|:-------------|:----:|:--------------|:--------------|
| v0.17.0 | 2015-11-30   | 7    | 3.5.1         | 5.0.0-beta-17 |
| v0.16.1 | 2013-10-30   | 6    | 3.2.4         | 4.1.0         |
| v0.16.0 | 2012-03-06   | 6    | 3.2.4         | 4.1.0         |


### v0.17.0
*(2015-11-30)*
* uses Apache Maven to create the release
* uses jcel 0.22.0
* runs on Java 7
* uses the OWL API 3.5.1, and can be used as a plug-in for Protégé 5.0.0-beta-17 .
* build commands:
```
$ mvn clean install
```
* plug-in: `gel/target/de.tu-dresden.inf.lat.gel-0.17.0.jar`


### v0.16.1
*(2013-10-30)*
* has been refactored and got superfluous code removed
* uses jcel 0.16.0
* runs on Java 6
* uses the OWL API 3.2.4, and can be used as a plug-in for Protégé 4.1.0 .
* build commands:
```
$ cd de.tudresden.inf.lat.gel
$ ant
```
* plug-in: `de.tudresden.inf.lat.gel/dist/bundle/de.tudresden.inf.lat.gel.jar`


### v0.16.0
*(2012-03-06)*
* first operation version
* uses jcel 0.16.0
* runs on Java 6
* uses the OWL API 3.2.4, and can be used as a plug-in for Protégé 4.1.0 .
* build commands:
```
$ ant
```
* plug-in: `de.tudresden.inf.lat.gel/dist/bundle/de.tudresden.inf.lat.gel.jar`



