
# Java Barnes Hut code

A simple Java code illustrating use of Barnes Hut algorithm for N-body simulation of a group of bodies - "stars" - interacting through the force of gravity.

## Getting Started

### Prequisites

Java JDK and Maven 3 for the build.

### Installing and running

Download distribution from GIT.

In the project folder, build by:
```
  $ mvn
```
Run by:
```
  $ java -cp target/java-bh-1.0-SNAPSHOT.jar org.hpjava.BarnesHut
```
A Java graphics window should appear to display current state of
simulation.  Monitoring output including profiling information will be printed at the terminal.

## Disclaimer

Although the logic of this code is believed to be a correct implementatio of Barnes-Hut, parameters such as the time step have not been tuned to guarantee accuracy of the simulation.

