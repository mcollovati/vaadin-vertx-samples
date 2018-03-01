example app
==============

This is a sample Vaadin application that runs on Vertx.


Project Structure
=================

The project consists of the following three modules:

- parent project: common metadata and configuration
- example-app-widgetset: widgetset, custom client side code and dependencies to widget add-ons
- example-app-ui: main application module, development time
- exmple-app-backed: simulation of a backed service

Workflow
========

To compile the entire project, run "mvn install" in the parent project.

Other basic workflow steps:

- getting started
- compiling the whole project
  - run "mvn install" in parent project
- developing the application
  - edit code in the ui module
  - run "mvn package" in ui module
  - run "java -jar target/example-app-ui-1.0-SNAPSHOT-fat.jar"
  - open http://localhost:8080/
- client side changes or add-ons
  - edit code/POM in widgetset module
  - run "mvn install" in widgetset module
  - if a new add-on has an embedded theme, run "mvn vaadin:update-theme" in the ui module
- debugging client side code
  - run "mvn vaadin:run-codeserver" in widgetset module
  - activate Super Dev Mode in the debug window of the application
- clustering, HA and fail over
  - run "mvn package -Pha-hazelcast" in ui module
  - in a terminal run run "java -jar target/example-app-ui-1.0-SNAPSHOT-fat.jar -ha"
  - in other terminals run run "java -jar target/example-app-ui-1.0-SNAPSHOT-fat.jar bare"
  - open http://localhost:8080/
  - navigate application and after a while kill the first terminal (`kill -9`)
  - after a few seconds the verticle will be redeployed on another node and application will automatically reconnect   

Developing a theme using the runtime compiler
-------------------------

When developing the theme, Vaadin can be configured to compile the SASS based
theme at runtime in the server. This way you can just modify the scss files in
your IDE and reload the browser to see changes.

To use on the runtime compilation, open pom.xml of your UI project and comment 
out the compile-theme goal from vaadin-maven-plugin configuration. To remove 
an existing pre-compiled theme, remove the styles.css file in the theme directory.

When using the runtime compiler, running the application in the "run" mode 
(rather than in "debug" mode) can speed up consecutive theme compilations
significantly.

The production module always automatically precompiles the theme for the production WAR.

Using Vaadin pre-releases
-------------------------

If Vaadin pre-releases are not enabled by default, use the Maven parameter
"-P vaadin-prerelease" or change the activation default value of the profile in pom.xml .
