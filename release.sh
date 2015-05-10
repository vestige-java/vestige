#!/bin/sh

mvn -DpreparationGoals="clean install" -DaddSchema=false -DautoVersionSubmodules=true release:clean release:prepare && mvn release:perform;
