#!/usr/bin/env bash

HADOOP_HOME=${HADOOP_HOME:-/home/ilya/hadoop-2.7.3}
SPARK_HOME=${SPARK_HOME:-/home/ilya/spark-2.1.0-bin-hadoop2.7}

export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop

for (( i = 0; i < 3; i++ )); do

    echo "Executing run $i..."

    RUN_NAME=run$(printf %02d $i)

    $SPARK_HOME/bin/spark-submit \
    --master yarn \
    --deploy-mode cluster \
    --executor-cores 8 \
    --properties-file conf/spark-defaults.conf \
    --class de.tuberlin.cit.jobs.KMeans \
    ../target/runtime-adjustments-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar \
    --min-containers 4 \
    --max-containers 24 \
    --max-runtime 360000 \
    --iterations 10 \
    --k 8 \
    --db "/home/ilya/runtime-adjustments-experiments/target/bell" \
    hdfs://wally046:45010//kmeans.txt \
    > logs/${RUN_NAME}.out 2> logs/${RUN_NAME}.log

done
