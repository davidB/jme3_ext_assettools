**IT'S A DRAFT , WIP**

# Package

I name a jme3 package:

- a regular jar/zip file, with code (*.class), resources (*.j3o, *.j3m, *.png,...)
- meta-data (.pom, repository info, updates.xml) : tags, description, url, **dependencies**

The following solution can be extended to support SDK plugin ([*.nbm](http://wiki.netbeans.org/DevFaqWhatIsNbm)).

## Conventions

(Proposal)

* Files :
  * required : <name>-<version>.jar (eg: mystuff-0.1.0.jar)
  * required : <name>-<version>.pom (eg: mystuff-0.1.0.pom)
  * optional : <name>-<version>-sources.jar (eg: mystuff-0.1.0-sources.jar)
  * optional : <name>-<version>-javadoc.jar (eg: mystuff-0.1.0-javadoc.jar)
* Jar layouts
* Pom metadata
  * it's a xml file (full description)
  * a minimal template for your mystuff.pom
    ```
    <!-- TODO -->
    ```
* Bintray metadata
* Bintray aggregated repository : https://bintray.com/jmonkeyengine/contrib

* Create an archive (eg: mystuff.jar) with your files
  * for resources, follow the directory layout of the asset dir (Models/..., Textures/...)
  * for code

# Bintray ??

Bintray is a hosting service for packages (maven, generic, apt, rpm).
Take a look at [overview from bintray](https://bintray.com/howbintrayworks#page1).

I'm aware about :
* jme-contribution (nbm only, svn or the wip next version)
* the asset-pack
* the investigation about an "assets' service", made by core team

you can call me fanboy, but I don't see the need to re-invent the wheel and create a "specific" service for the jme community. When there is an already existing solution, used by some other communnities (eg: gradle, gradle-plugins, maven). And the future "assets'service" may wrap bintray.

A solution where you're not locked into (except the REST api):

* users are free to remove its assets from bintray
* you can create a compatible client by using the REST api
* you can create our own hostint by implementing the REST api (and keep existing client)
* you are always free to go away

## Why ?

* it's like github for binaries (user centric, organisation)
* package owner is free to add, edit, remove their packages and versions
* it's free for OSS package (license required)
* provide REST api, CDN, starring, statistics, [...](https://bintray.com/)
* provide a commercial service for more

# Storing Package in bintray

## With SDK

Today, there is no plugin, so you should use a other solution (eg: webui, gradle)
Bintray offer a REST api, so it's doable to create a plugin to ease upload from SDK. (tell me if you need).

## With WebUI

### Initialization

1. Register to [bintray](https://bintray.com/), I recommand to register with your github account.
2. Create a repository (of kind "maven") or use the default "maven" to store your jme package.

### Per Package

#### Prepare the package

1. Create an archive (eg: mystuff.jar) with your files
  * for resources, follow the directory layout of the asset dir (Models/..., Textures/...)
  * for code
1. Create the metadata pom (eg: mystuff.pom)
  * it's a xml file (full description)
  * a minimal template to use
    ```
    <!-- TODO -->
    ```

#### Upload the package

1. Select your repository
2. Add a new package + fill the form
3. Add a new version + fill the form
4. upload files

## With gradle

Part of the previous workflow can be automatized.

### Initialization

* same as WebUI (see above)
* install gradle

### Per Package

#### Prepare the package

sample : [assets_publisher0]()

#### Upload the package

Apply the gradle plugins :

* maven or maven-plublish
* bintray
  * issue : not verbose in case of error like missing of wrong field (eg: license are required and member of pre-defined list (see web form))
  * issue : package's attributes can't be updated, only set at creation time (use the web form to edit them)

sample : [assets_publisher0]()

# Using Package

## With SDK

Today, there is no plugin to ease the process (tell me your suggestion).

## With gradle
