package de.tuberlin.cit.datagens

import java.io.PrintWriter
import java.util.concurrent.ThreadLocalRandom

import scala.math.pow

object SGDDataGenerator {
  def main(args: Array[String]): Unit = {
    if (args.length != 3) {
      Console.err.println("Usage: SGDDataGenerator <samples> <dimension> <output>")
      System.exit(-1)
    }

    val m = args(0).toInt
    val n = args(1).toInt
    val outputPath = args(2)

    val writer = new PrintWriter(outputPath)

    for (_ <- 0 until m) {
      val x = ThreadLocalRandom.current().nextDouble() * 10
      val noise = ThreadLocalRandom.current().nextGaussian() * 3

      // generate the function value with added gaussian noise
      val label = function(x) + noise

      // generate a vandermatrix from x
      val vector = polyvander(x, n - 1)

      // write to file in MLUtils format (label, feature1 feature2 ... featureN)
      writer.println(label + "," + vector.mkString(" "))
    }

    writer.flush()
    writer.close()
  }

  def polyvander(x: Double, order: Int): Array[Double] = {
    (0 to order).map(pow(x, _)).toArray
  }

  def function(x: Double): Double = {
    2 * x + 10
  }
}
