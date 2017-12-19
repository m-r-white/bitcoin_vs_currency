// Databricks notebook source
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{RandomForestRegressor, LinearRegression, GeneralizedLinearRegression}
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.sql.types._
import org.apache.spark.sql.SQLContext._

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.VectorIndexer
import org.apache.spark.ml.regression.{GBTRegressionModel, GBTRegressor}

// COMMAND ----------

// Load the dataset
var df = sqlContext
  .read
  .format("csv")
  .option("inferSchema", "true")
  .option("header", "true")
  .load("/FileStore/tables/ct1.csv")

// COMMAND ----------

df.printSchema

// COMMAND ----------

df.show

// COMMAND ----------

// Create features column
val assembler = new VectorAssembler()
  .setInputCols(Array("BTC_CryptoCompare", "BTC_Twitter", "BTC_Reddit", "BTC_Facebook", "BTC_GitHub"))
  .setOutputCol("features")

// COMMAND ----------

// Create label column (target)
df = df.withColumnRenamed("btc_pr", "label")

// COMMAND ----------

df = assembler.transform(df)

// COMMAND ----------

// train test split
val Array(train, test) = df.randomSplit(Array(.8, .2), 2)

// COMMAND ----------

train.show()

// COMMAND ----------

// RFR

// COMMAND ----------

var rf = new RandomForestRegressor()
  .setNumTrees(35)

// COMMAND ----------

var rfModel = rf.fit(train)

// COMMAND ----------

var predictions = rfModel.transform(test)

// COMMAND ----------

predictions.show()

// COMMAND ----------

val evaluator = new RegressionEvaluator()
  .setMetricName("r2")

// COMMAND ----------

evaluator.evaluate(predictions)

// COMMAND ----------

// get Baseline
// 100 = 0.8587451826642822
// 30  = 0.8616094606328323

// COMMAND ----------

predictions.select("prediction", "label", "features").show(500)

// COMMAND ----------



// COMMAND ----------



// COMMAND ----------

// GLR

// COMMAND ----------

val glr = new GeneralizedLinearRegression()
  .setFamily("gaussian")
  .setLink("identity")
  .setMaxIter(10)
  .setRegParam(0.3)

// COMMAND ----------

val model = glr.fit(train)

// COMMAND ----------

println(s"Coefficients: ${model.coefficients}")
println(s"Intercept: ${model.intercept}")

// COMMAND ----------

val summary = model.summary
println(s"Coefficient Standard Errors: ${summary.coefficientStandardErrors.mkString(",")}")
println(s"T Values: ${summary.tValues.mkString(",")}")
println(s"P Values: ${summary.pValues.mkString(",")}")
println(s"Dispersion: ${summary.dispersion}")
println(s"Null Deviance: ${summary.nullDeviance}")
println(s"Residual Degree Of Freedom Null: ${summary.residualDegreeOfFreedomNull}")
println(s"Deviance: ${summary.deviance}")
println(s"Residual Degree Of Freedom: ${summary.residualDegreeOfFreedom}")
println(s"AIC: ${summary.aic}")
println("Deviance Residuals: ")
summary.residuals().show()

// COMMAND ----------

// GBT

// COMMAND ----------

// Automatically identify categorical features, and index them.
// Set maxCategories so features with > 4 distinct values are treated as continuous.
val featureIndexer = new VectorIndexer()
  .setInputCol("features")
  .setOutputCol("indexedFeatures")
  .setMaxCategories(4)
  .fit(data)

// COMMAND ----------

val gbt = new GBTRegressor()
  .setLabelCol("label")
  .setFeaturesCol("indexedFeatures")
  .setMaxIter(10)

// COMMAND ----------

var model = gbt.fit(train)

// COMMAND ----------

// Make predictions.
val predictions = model.transform(test)

// COMMAND ----------

// Select example rows to display.
predictions.select("prediction", "label", "features").show(5)

// COMMAND ----------

// Select (prediction, true label) and compute test error.
val evaluator = new RegressionEvaluator()
  .setLabelCol("label")
  .setPredictionCol("prediction")
  .setMetricName("rmse")
val rmse = evaluator.evaluate(predictions)
println("Root Mean Squared Error (RMSE) on test data = " + rmse)""

// COMMAND ----------

val gbtModel = model.stages(1).asInstanceOf[GBTRegressionModel]
println("Learned regression GBT model:\n" + gbtModel.toDebugString)

// COMMAND ----------


