package de.tuberlin.cit.jobs

import de.tuberlin.cit.adjustments.StageScaleOutPredictor
import org.apache.spark.mllib.optimization.SquaredL2Updater
import org.apache.spark.mllib.regression.LinearRegressionWithSGD
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.{SparkConf, SparkContext}
import org.rogach.scallop.exceptions.ScallopException
import org.rogach.scallop.{ScallopConf, ScallopOption}

object SGD {

  def main(args: Array[String]): Unit = {

    val conf = new SGDArgs(args)

    val appSignature = "SGD"

    val sparkConf = new SparkConf()
      .setAppName(appSignature)
    //      .set("spark.shuffle.service.enabled", "true")
    //      .set("spark.dynamicAllocation.enabled", "true")

    val sparkContext = new SparkContext(sparkConf)
    val listener = new StageScaleOutPredictor(
      sparkContext,
      appSignature,
      conf.dbPath(),
      conf.minContainers(),
      conf.maxContainers(),
      conf.maxRuntime().toInt,
      conf.adaptive())
    sparkContext.addSparkListener(listener)


    var trainingSet = MLUtils.loadLabeledPoints(sparkContext, conf.input())
    if (conf.cache()) {
      trainingSet = trainingSet.cache()
    }

    val numIterations = conf.iterations()
    val stepSize = 1.0
    val regParam = 0.01

    val algorithm = new LinearRegressionWithSGD()
    algorithm.optimizer
      .setNumIterations(numIterations)
      .setStepSize(stepSize)
      .setUpdater(new SquaredL2Updater())
      .setRegParam(regParam)

    val model = algorithm.run(trainingSet)
    println(model.weights)

    sparkContext.stop()
  }
}
class SGDArgs(a: Seq[String]) extends ScallopConf(a) {
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
