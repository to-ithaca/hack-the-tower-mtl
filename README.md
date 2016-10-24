# MTL workshop

## Overview

These are the resources for the MTL programming in scala workshop. Each of the subsequent steps in the workshop are a different branch in the project. The branches are meant to be followed in order; changes from branch to branch will be explained in the README. 

## Getting started

The project assumes that the user already has [sbt](https://github.com/sbt) and [git](https://git-scm.com/) setup on their system. 

To compile the core project from the command line use,

```
sbt compile
```

and for the tests,

```
sbt test
```

This project uses [typelevel scala](https://github.com/typelevel/scala) which is a fork of the scala compiler with some additional features. All of the features used within this workshop can be added using various plugins to the Lightbend's scala compiler.

## Setting the scene

You've inherited a code base from a collegue who has long since vanished to find his true calling as a clojure developer. The code base was half scripted and half object oriented since it was written in 2012 (and by a clojure developer). It's now 2016 and functional programming is in vogue you've been asked by your skeptical colleagues to show them the error of their ways and the practical benefits of burritos and monads, concepts you've been preaching for the past 3 weeks.

Undeterred you step into this brave new world...
