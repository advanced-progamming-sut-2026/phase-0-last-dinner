# Plants vs. Zombies 2

Advanced Programming course project.

## Requirements

- JDK 17 or newer
- Maven 3.8 or newer

## Run

```powershell
mvn compile
mvn exec:java -Dexec.mainClass="ir.sharif.pvz.Main"
```

You can also open the repository as a Maven project in IntelliJ IDEA and run
`ir.sharif.pvz.Main`.

## Current Structure

```text
src/main/java/ir/sharif/pvz/
  Main.java
  controller/
  model/
  util/
  view/
```

The first commit only contains a small MVC application shell. Game features
will be added incrementally after the initial UML and domain design are ready.
