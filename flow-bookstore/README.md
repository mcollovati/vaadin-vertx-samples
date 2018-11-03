# Bookstore App Starter for Vaadin Flow running on Vert,x

A project example for a Vaadin application that runs on Vert.x. The UI is built with Java only.


## Prerequisites

The project can be imported into the IDE of your choice, with Java 8 installed, as a Maven project.

## Project Structure

The project consists of the following three modules:

- parent project: common metadata and configuration
- bookstore-starter-flow-ui: main application module, development time
- bookstore-starter-flow-backend: POJO classes and mock services being used in ui

## Workflow

To compile the entire project, run "mvn install" in the parent project.

Other basic workflow steps:

- getting started
- compiling the whole project
  - run `mvn install` in parent project
- developing the application
  - edit code in the ui module
  - run `mvn package` in ui module
  - run `mvn vertx:run` or `java -jar target/bookstore-ui-1.0-SNAPSHOT-fat.jar` in ui module
  - open http://localhost:8080/

