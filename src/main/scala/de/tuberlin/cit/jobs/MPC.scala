package de.tuberlin.cit.jobs

import de.tuberlin.cit.adjustments.StageScaleOutPredictor
import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.ml.feature.{LabeledPoint => NewLabeledPoint}
import org.apache.spark.{SparkConf, SparkContext}
import org.rogach.scallop.exceptions.ScallopException
import org.rogach.scallop.{ScallopConf, ScallopOption}

object MPC {
  def main(args: Array[String]): Unit = {


    val conf = new MPCArgs(args)

    val appSignature = "MPC"

    val sparkConf = new SparkConf()
      .setAppName(appSignature)

    val spark = SparkSession
      .builder
      .appName(appSignature)
      .config(sparkConf)
      .getOrCreate()
    import spark.implicits._

    val listener = new StageScaleOutPredictor(
      spark.sparkContext,
      appSignature,
      conf.dbPath(),
      conf.minContainers(),
      conf.maxContainers(),
      conf.maxRuntime().toInt,
      conf.adaptive())
    spark.sparkContext.addSparkListener(listener)

    val data = MLUtils.loadLabeledPoints(spark.sparkContext, conf.input()).map(lp => {
      NewLabeledPoint(lp.label, lp.features.asML)
    }).toDF()

    // Split the data into train and test
    val splits = data.randomSplit(Array(0.6, 0.4), seed = 1234L)
    val train = splits(0)
    val test = splits(1)

    // specify layers for the neural network:
    // input layer of size 4 (features), two intermediate of size 5 and 4
    // and output of size 3 (classes)
    val layers = Array[Int](4, 5, 4, 3)

    // create the trainer and set its parameters
    val trainer = new MultilayerPerceptronClassifier()
      .setLayers(layers)
      .setBlockSize(128)
      .setSeed(1234L)
      .setMaxIter(conf.iterations())

    // train the model
    val model = trainer.fit(train)

    // compute accuracy on the test set
    val result = model.transform(test)
    val predictionAndLabels = result.select("prediction", "label")
    val evaluator = new MulticlassClassificationEvaluator()
      .setMetricName("accuracy")

    println("Test set accuracy = " + evaluator.evaluate(predictionAndLabels))

    spark.stop()
  }
}

class MPCArgs(a: Seq[String]) extends ScallopConf(a) {
  //  val config = opt[String](required = true,
  //    descr = "Path to the .conf file")
  //
  val dbPath: ScallopOption[String] = opt[String](name = "db", noshort = true, descr = "Path to the H2 database", default = Some("./target/bell"))
  val input: ScallopOption[String] = trailArg[String](required = true, name = "<input>",
    descr = "Input file").map(_.toLowerCase)
  val maxRuntime: ScallopOption[Double] = opt[Double](required = true, short = 'r',
    descr = "Maximum runtime in seconds")
  val minContainers: ScallopOption[Int] = opt[Int](short = 'n', default = Option(1),
    descr = "Minimum number of containers to assign")
  val maxContainers: ScallopOption[Int] = opt[Int](required = true, short = 'N',
    descr = "Maximum number of containers to assign")

  //  val memory = opt[Int](
  //    descr = "Memory per container, in MB")
  //  val masterMemory = opt[Int](short = 'M',
  //    descr = "Master memory, in MB (Flink JobManager or Spark driver)")
  //  val slots = opt[Int](
  //    descr = "Number of slots per TaskManager")
  val iterations: ScallopOption[Int] = opt[Int](noshort = true, default = Option(100),
    descr = "Amount of SGD iterations")
  val cache: ScallopOption[Boolean] = opt[Boolean](noshort = true, default = Option(false),
    descr = "Caches the input data")
  val adaptive: ScallopOption[Boolean] = opt[Boolean](default = Option(false), noshort = true,
    descr = "Enables runtime adjustments by scaling between jobs")

  override def onError(e: Throwable): Unit = e match {
    case ScallopException(message) =>
      println(message)
      //      println(s"Usage: allocation-assistant -c <config> -r <max runtime> -m <memory> -s <slots> " +
      //        s"-i <fallback containers> -N <max containers> " +
      //        s"[more args ...] <Jar> [Jar args ...]")
      println()
      printHelp()
      System.exit(1)
    case other => super.onError(e)
  }

  verify()
}
