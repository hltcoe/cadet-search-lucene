#!/bin/bash

mvn $@ package dependency:copy-dependencies
