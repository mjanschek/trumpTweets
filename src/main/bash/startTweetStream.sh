#!/bin/bash

TWEETDIR=/home/garg/tweets/
SPARKDIR=/home/garg/spark-2.2.0-bin-hadoop2.7/

$SPARKDIR/sbin/start-all.sh

$SPARKDIR/bin/spark-submit --packages org.apache.bahir:spark-streaming-twitter_2.11:2.2.0 $TWEETDIR/TrumpTweets-0.1.jar 