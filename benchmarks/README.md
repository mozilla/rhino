# Rhino Benchmarks

This directory contains a collection of various benchmarks that have been added to the Rhino
project over the years. They have all been collected here and run using the JMH framework.

## Running the Benchmarks

To run all the benchmarks that exist, simply run, from the top-level directory:

    ./gradlew jmh

In addition, the environment variable BENCHMARK may be used to restrict which benchmarks are
run -- it is a regular expression that matches the names of the benchmarks. 

For example, to run only the SunSpider and V8 benchmarks, you can run:

    BENCHMARK=V8|SunSpider ./gradlew jmh

Running all the benchmarks takes about half an hour, so this is a valuable thing to do!

## How the Benchmarks work

Java, with its just-in-time compilation pipeline, bytecode generation in Rhino, and
Java's super-complex garbage collector, is sensitive to warm up time and as a result, Rhino has 
more variation between runs than other JavaScript engines. To get a repeatable result, we use
the JMH framework, which runs each benchmark many times and does other "black hole" protection
operations to try and get an accurate result. For this reason, these benchmarks take a lot longer
to run than your favorite on-line JavaScript benchmarking web site.

The purpose of these benchmarks has historically been to make Rhino perform better in server-side
environments, so they all run at the maximum optimization level (9). Since so many people also
use Rhino in interpreted mode, if there's an interest in benchmarking that too then we can
always adjust these tests.

## Benchmark Notes

Here are a few notes on the specific benchmarks:

* **SunSpiderBenchmark**: These are the venerable JavaScript benchmarks that have been used
for a long time. They test both low-level operations like math operations, as well as higher-level
tasks.
* **V8Benchmark**: These are Google's V8 benchmarks, which may have been created to show how 
efficient V8 is. They are still a good way to show how far we have to go.
* **SlotMapBenchmark**: This is a low-level benchmark of the various SlotMap types in Rhino.
Tiny changes in SlotMap performance affect property access in Rhino and translate into big
changes in the other benchmarks.
* **PropertyBenchmark**: This is a micro-benchmark that uses JavaScript code to test the efficiency
of object property access.
* **ObjectBenchmark**: These serve a similar purpose to PropertyBenchmark and perhaps we should
have deleted these by now.
* **BuiltinBenchmark**: This tries to measure the relative performance of the various ways to create
native JavaScript objects in Java -- the reflection-based method, the IdScriptableObject that is used
for many internal objects, and lambda functions.
* **MathBenchmark**: This is a vehicle for quickly testing low-level math operations.