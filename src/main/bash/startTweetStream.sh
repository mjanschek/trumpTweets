#!/bin/bash

WORKDIR=.
SPARKDIR=~/spark-2.2.0-bin-hadoop2.7/

$SPARKDIR/sbin/start-all.sh

$SPARKDIR/bin/spark-submit --packages=org.apache.bahir:spark-streaming-twitter_2.11:2.2.0,com.opencsv:opencsv:4.1 --driver-memory 4G $WORKDIR/TrumpTweets-0.1.jar 

$SHELL
