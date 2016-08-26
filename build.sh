#!/usr/bin/env sh

mvn $@ package dependency:copy-dependencies
