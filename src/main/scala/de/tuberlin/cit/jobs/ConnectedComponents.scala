package de.tuberlin.cit.jobs

import de.tuberlin.cit.adjustments.StageScaleOutPredictor
import org.apache.spark.graphx.{Graph, VertexId}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.rogach.scallop.exceptions.ScallopException
import org.rogach.scallop.{ScallopConf, ScallopOption}

object ConnectedComponents {
  def main(args: Array[String]): Unit = {

    val conf = new ConnectedComponentsArgs(args)
    val appSignature = "CC"

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

    sparkContext.textFile(conf.input())
    val data = sparkContext.textFile(conf.input())
    val edges: RDD[(VertexId, VertexId)] = data.map(s => {
      val arr = s.split("\\s").map(_.toLong)
      (arr(0), arr(1))
    })
    val graph: Graph[Int, Int] = Graph.fromEdgeTuples(edges, 1)
    val result: Graph[VertexId, Int] = graph.connectedComponents(conf.iterations())

    val largestComponentSize = result.vertices
      .map(_.swap)
      .mapValues(_ => 1L)
      .reduceByKey(_ + _)
      .sortBy(_._2, ascending = false)
      .first()
      ._2
    println(s"Largest component size: $largestComponentSize")
  }
}

class ConnectedComponentsArgs(a: Seq[String]) extends ScallopConf(a) {
  //  val config = opt[String](required = true,
  //    descr = "Path to the .conf file")
  //
  val dbPath: ScallopOption[String] = opt[String](name = "db", noshort = true, descr = "Path to the H2 database",
    default = Some("./target/bell"))
  val input: ScallopOption[String] = trailArg[String](required = true, name = "<input>",
    descr = "Input file").map(_.toLowerCase)
  val maxRuntime: ScallopOption[Int] = opt[Int](required = true, short = 'r',
    descr = "Maximum runtime in milliseconds")
  val minContainers: ScallopOption[Int] = opt[Int](short = 'n', default = Option(1),
    descr = "Minimum number of containers to assign")
  val maxContainers: ScallopOption[Int] = opt[Int](required = true, short = 'N',
    descr = "Maximum number of containers to assign")

  val iterations: ScallopOption[Int] = opt[Int](noshort = true, default = Option(100),
    descr = "Amount of KMeans iterations")
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

