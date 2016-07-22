# Run samples

Compile and package app as a fat jar

```
mvn clean package
```

## Simple UI sample

```
cd target
java -jar sample-<version>-fat.jar -conf=../src/main/conf/simple.conf
```

Open your browser at http://localhost:9090/simple/


## PUSH sample

```
cd target
java -jar sample-<version>-fat.jar run com.github.mcollovati.vertx.vaadin.sample.PushTestUI\$MyVerticle
```

Open your browser at http://localhost:8080/

## High Availability and Fail-Over

Package with clustering support

```
mvn -Pha clean package
```

Run first app in a terminal

```
java -jar sample-<version>-fat.jar run com.github.mcollovati.vertx.vaadin.sample.PushTestUI\$MyVerticle -ha
```

Open another terminal and run second app

```
java -jar sample-<version>-fat.jar run com.github.mcollovati.vertx.vaadin.sample.SimpleVerticle -ha
```

Open your browser at http://localhost:9090/simple/ and http://localhost:8080/: applications are running on separate nodes.

Kill the process running simple UI; after few seconds the app will be deployed on first node.


