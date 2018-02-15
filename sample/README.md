# Run samples

Compile and package app as a fat jar

### Maven build

```
mvn clean package
```

### Gradle build

```
./gradlew clean shadowJar
```

## Simple UI sample

### Maven build

```
cd target
java -jar sample-1.0-SNAPSHOT-fat.jar -conf=../src/main/conf/simple.conf
```


### Gradle build

```
cd build/libs
java -jar sample-1.0-SNAPSHOT-all.jar -conf=../../src/main/conf/simple.conf
```


Open your browser at http://localhost:9090/simple/


## PUSH sample

### Maven build

```
cd target
java -jar sample-1.0-SNAPSHOT-fat.jar run com.github.mcollovati.vertx.vaadin.sample.PushTestUI\$MyVerticle
```

### Gradle build

```
cd build/libs
java -jar sample-1.0-SNAPSHOT-all.jar run com.github.mcollovati.vertx.vaadin.sample.PushTestUI\$MyVerticle
```


Open your browser at http://localhost:8080/

## High Availability and Fail-Over

Package with clustering support

### Maven build

```
mvn -Pha clean package
cd target
```

Run first app in a terminal

```
java -jar sample-1.0-SNAPSHOT-fat.jar run com.github.mcollovati.vertx.vaadin.sample.PushTestUI\$MyVerticle -ha
```

Open another terminal and run second app


```
java -jar sample-1.0-SNAPSHOT-fat.jar run com.github.mcollovati.vertx.vaadin.sample.SimpleVerticle -ha
```


### Gradle build

```
./gradlew clean -Pha=true shadowJar
cd build/libs
```

Run first app in a terminal

```
java -jar sample-1.0-SNAPSHOT-all.jar run com.github.mcollovati.vertx.vaadin.sample.PushTestUI\$MyVerticle -ha
```

Open another terminal and run second app

```
java -jar sample-1.0-SNAPSHOT-all.jar run com.github.mcollovati.vertx.vaadin.sample.SimpleVerticle -ha
```

### Testing failover

Open your browser at http://localhost:9090/simple/ and http://localhost:8080/: applications are running on separate nodes.

Kill the process running simple UI; after few seconds the app will be deployed on first node.


