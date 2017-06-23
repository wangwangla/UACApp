#!/usr/bin/env bash

# find voltdb binaries
if [ -e ../../bin/voltdb ]; then
    # assume this is the examples folder for a kit
    VOLTDB_BIN="$(dirname $(dirname $(pwd)))/bin"
elif [ -n "$(which voltdb 2> /dev/null)" ]; then
    # assume we're using voltdb from the path
    VOLTDB_BIN=$(dirname "$(which voltdb)")
else
    echo "Unable to find VoltDB installation."
    echo "Please add VoltDB's bin directory to your path."
    exit -1
fi

# call script to set up paths, including
# java classpaths and binary paths
source $VOLTDB_BIN/voltenv

# leader host for startup purposes only
# (once running, all nodes are the same -- no leaders)
STARTUPLEADERHOST="localhost"
# list of cluster nodes separated by commas in host:[port] format
SERVERS="localhost"

# remove binaries, logs, runtime artifacts, etc... but keep the jars
function clean() {
    rm -rf voltdbroot \
           log \
           src/HuaweiUACApp/*.class \
           src/HuaweiUACApp/compiler/*.class \
           src/HuaweiUACApp/procedures/*.class \
           *.log
}

# remove everything from "clean" as well as the jarfiles
function cleanall() {
    clean
    rm -rf *.jar
}

# compile the source code for procedures and the client into jarfiles
function jars() {
    # compile java source
    javac -classpath lib/voltdb-6.6.7.jar:lib/voltdbclient-6.6.7.jar \
        src/HuaweiUACApp/*.java \
        src/HuaweiUACApp/compiler/*.java \
        src/HuaweiUACApp/procedures/*.java
    # build procedure and client jars
    jar cf HuaweiUACApp.jar -C src HuaweiUACApp
    # remove compiled .class files
    rm -rf src/HuaweiUACApp/*.class \
           src/HuaweiUACApp/compiler/*.class \
           src/HuaweiUACApp/procedures/*.class
}

# compile the procedure and client jarfiles if they don't exist
function jars-ifneeded() {
    if [ ! -e HuaweiUACApp.jar ]; then
        jars;
    fi
}

# Init to directory voltdbroot
function voltinit-ifneeded() {
    voltdb init --force
}

# run the voltdb server locally
function server() {
    jars-ifneeded
    voltinit-ifneeded
    voltdb start -H $STARTUPLEADERHOST
}

# load schema and procedures
function init() {
    jars-ifneeded
    sqlcmd < ddl.sql
}

# run the client that drives the example
function client() {
    async-benchmark
}

# Asynchronous benchmark sample
# Use this target for argument help
function async-benchmark-help() {
    jars-ifneeded
    java -classpath HuaweiUACApp.jar:$CLIENTCLASSPATH HuaweiUACApp.ReproducerApp --help
}

# latencyreport: default is OFF
# ratelimit: must be a reasonable value if lantencyreport is ON
# Disable the comments to get latency report
function async-benchmark() {
    jars-ifneeded
    java -classpath lib/voltdb-6.6.7.jar:lib/voltdbclient-6.6.7.jar:HuaweiUACApp.jar HuaweiUACApp.ReproducerApp \
        --displayinterval=5 \
        --warmup=5 \
        --datasize=1000 \
        --duration=2147483646 \
        --servers=$SERVERS | tee applog.log
}

function help() {
    echo "Usage: ./run.sh {clean|cleanall|jars|server|init|client|async-benchmark|aysnc-benchmark-help|...}"
    echo "       {...|sync-benchmark|sync-benchmark-help|jdbc-benchmark|jdbc-benchmark-help|simple-benchmark}"
}

# Run the targets pass on the command line
# If no first arg, run server
if [ $# -eq 0 ]; then server; exit; fi
for arg in "$@"
do
    echo "${0}: Performing $arg..."
    $arg
done
