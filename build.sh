#!/bin/sh -ex

echo "[INFO] Compiling asrec.groovy to classes/"
groovyc -d classes asrec.groovy
echo "[INFO] Packaging asrec.jar"
jar cvfm asrec.jar manifest -C classes/ .
echo "[INFO] Asrec build succesfully"

if [ "$1" = "test" ]; then
	echo "[INFO] Starting asrec.jar"
	java -jar asrec.jar &
	pid="$!"; sleep 20
	kill "$pid"
fi
