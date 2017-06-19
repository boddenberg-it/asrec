#!/bin/sh -ex
echo "[INFO] Compiling asrec.groovy to classes/"
groovyc -d classes asrec.groovy
echo "[INFO] Packaging asrec.jar"
jar cvfm asrec.jar manifest -C classes/ .
echo "[INFO] asrec build succesfully"
