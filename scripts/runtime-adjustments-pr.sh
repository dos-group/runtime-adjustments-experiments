#!/usr/bin/env bash

HADOOP_HOME=${HADOOP_HOME:-/home/ilya/hadoop-2.7.3}
SPARK_HOME=${SPARK_HOME:-/home/ilya/spark-2.1.0-bin-hadoop2.7}

export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop

for (( i = 0; i < 10; i++ )); do

    echo "Executing run $i..."
    $HADOOP_HOME/bin/hdfs dfs -rm -R /pr-out

    RUN_NAME=run$(printf %02d $i)

    $SPARK_HOME/bin/spark-submit \
    --master yarn \
    --deploy-mode cluster \
    --executor-cores 8 \
    --properties-file conf/spark-defaults.conf \
    --class de.tuberlin.cit.jobs.PageRank \
    ../target/runtime-adjustments-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar \
    --min-containers 10 \
    --max-containers 50 \
    --max-runtime 600000 \
    --iterations 10 \
    --adaptive \
    --db "tcp://130.149.249.30:9092/~/bell" \
    hdfs://wally020:45010//twitter_rv.net \
    hdfs://wally020:45010//pr-out \
    > logs/${RUN_NAME}.out 2> logs/${RUN_NAME}.log

done
