package de.tuberlin.cit.jobs

import de.tuberlin.cit.adjustments.StageScaleOutPredictor
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.graphx.GraphLoader
import org.rogach.scallop.exceptions.ScallopException
import org.rogach.scallop.{ScallopConf, ScallopOption}

object PageRank {

  def main(args: Array[String]) {

    val conf = new PageRankArgs(args)
    val appSignature = "PageRank"

    val sparkConf = new SparkConf()
      .setAppName(appSignature)
    val sparkContext = new SparkContext(sparkConf)
    val listener = new StageScaleOutPredictor(
      sparkContext,
      appSignature,
      conf.dbPath(),
      conf.minContainers(),
      conf.maxContainers(),
      conf.maxRuntime(),
      conf.adaptive())
    sparkContext.addSparkListener(listener)

    val graph = GraphLoader.edgeListFile(sparkContext, conf.input())
    val pr = graph.staticPageRank(conf.iterations())

    pr.vertices.saveAsTextFile(conf.output())

  }

}

class PageRankArgs(a: Seq[String]) extends ScallopConf(a) {
  //  val config = opt[String](required = true,
  //    descr = "Path to the .conf file")
  //
  val dbPath: ScallopOption[String] = opt[String](name = "db", noshort = true, descr = "Path to the H2 database",
    default = Some("./target/bell"))
  val input: ScallopOption[String] = trailArg[String](required = true, name = "<input>",
    descr = "Input file").map(_.toLowerCase)
  val output: ScallopOption[String] = trailArg[String](required = true, name = "<output>",
    descr = "Output file").map(_.toLowerCase)
  val maxRuntime: ScallopOption[Int] = opt[Int](required = true, short = 'r',
    descr = "Maximum runtime in milliseconds")
  val minContainers: ScallopOption[Int] = opt[Int](short = 'n', default = Option(1),
    descr = "Minimum number of containers to assign")
  val maxContainers: ScallopOption[Int] = opt[Int](required = true, short = 'N',
    descr = "Maximum number of containers to assign")

  val iterations: ScallopOption[Int] = opt[Int](noshort = true, default = Option(100),
    descr = "Amount of iterations")
  val cache: ScallopOption[Boolean] = opt[Boolean](noshort = true, default = Option(false),
    descr = "Caches the input data")
  val adaptive: ScallopOption[Boolean] = opt[Boolean](default = Option(false), noshort = true,
    descr = "Enables adjustments by scaling between jobs")

  override def onError(e: Throwable): Unit = e match {
    case ScallopException(message) =>
      println(message)
      println()
      printHelp()
      System.exit(1)
    case other => super.onError(e)
  }

  verify()
}
