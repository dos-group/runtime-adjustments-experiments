/*
 * KMeans workload for BigDataBench
 */
package de.tuberlin.cit.jobs

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.{SparkConf, SparkContext}
import org.rogach.scallop.exceptions.ScallopException
import org.rogach.scallop.{ScallopConf, ScallopOption}

object KMeans {
  val MLLibKMeans = org.apache.spark.mllib.clustering.KMeans

  def main(args: Array[String]): Unit = {

    val conf = new KMeansArgs(args)
    val appSignature = "KMeans"


//    if (args.length < 3) {
//      //System.err.println("Usage: KMeans <master> <data_file> <k> <iterations> <save_path>" +
//      System.err.println("Usage: KMeans <data_file> <k> <iterations>" +
//        " [<slices>]")
//      System.exit(1)
//    }

    var splits = 2
    val sparkConf = new SparkConf()
      .setAppName(appSignature)
    val sc = new SparkContext(sparkConf)

//    val filename = args(0)
//    val k = args(1).toInt
//    val iterations = args(2).toInt
    // val save_file = args(4)
    //if (args.length > 5) splits = args(5).toInt
//    if (args.length > 3) splits = args(3).toInt

    println("Start KMeans training...")
    // Load and parse the data
    val data = sc.textFile(conf.input(), splits)
    val parsedData = data.map(s => Vectors.dense(s.split(' ').map(_.toDouble)))

    val clusters = MLLibKMeans.train(parsedData, conf.k(), conf.iterations())

    // Evaluate clustering by computing Within Set Sum of Squared Errors
    val WSSSE = clusters.computeCost(parsedData)
    println("Within Set Sum of Squared Errors = " + WSSSE)

    clusters.clusterCenters.foreach(v => {
      println(v)
    })
  }
}
class KMeansArgs(a: Seq[String]) extends ScallopConf(a) {
  //  val config = opt[String](required = true,
  //    descr = "Path to the .conf file")
  //
  val dbPath: ScallopOption[String] = opt[String](name = "db", noshort = true, descr = "Path to the H2 database",
    default = Some("./target/bell"))
  val input: ScallopOption[String] = trailArg[String](required = true, name = "<input>",
    descr = "Input file").map(_.toLowerCase)
  val maxRuntime: ScallopOption[Double] = opt[Double](required = true, short = 'r',
    descr = "Maximum runtime in seconds")
  val minContainers: ScallopOption[Int] = opt[Int](short = 'n', default = Option(1),
    descr = "Minimum number of containers to assign")
  val maxContainers: ScallopOption[Int] = opt[Int](required = true, short = 'N',
    descr = "Maximum number of containers to assign")

  val k: ScallopOption[Int] = opt[Int](required = true, descr = "Amount of clusters")
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

