
# Java Barnes Hut code

A simple Java code illustrating use of Barnes Hut algorithm for N-body simulation of a group of unit mass bodies - "stars" - interacting through the force of gravity.

## Getting Started

### Prequisites

Java 8 JDK and Maven 3 for the build.

### Installing and running

Download distribution from GitHub.

In the project folder, build by:
```
  $ mvn package
```
Run by:
```
  $ java -cp target/java-bh-1.0-SNAPSHOT.jar org.hpjava.BarnesHut
```
A Java graphics window should appear to display current state of
simulation.  Monitoring output including profiling information will be printed at the terminal.

The simulation will continue running until the graphics window is closed or the program is killed at the terminal.

## Disclaimer

Although the logic in this code is believed to be a correct implementation of Barnes-Hut, parameters including the time step and opening angle have not been tuned to guarantee accuracy of the simulation.

