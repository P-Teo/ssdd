package org.example

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.spark.SparkConf
import org.apache.spark.streaming.Durations
import org.apache.spark.streaming.api.java.JavaStreamingContext

fun main() {
    val sparkConf = SparkConf()
        .setMaster("local[2]")
        .setAppName("Spark StockMarket")

    val sparkContext = JavaStreamingContext(sparkConf, Durations.seconds(3))
    val lines = sparkContext.socketTextStream("localhost", 9999)

    // ... (codul de sus rămâne la fel)

    val processed = lines
        .map { line ->
            try {
                JsonParser().parse(line).asJsonObject
            } catch (e: Exception) {
                null
            }
        }
        .filter { obj -> obj != null }
        .map { obj -> obj!! }
        // 1. COMENTEAZĂ SAU ȘTERGE FILTRUL DE SURSĂ (temporar)
        // .filter { obj ->
        //     val source = if (obj.has("source") && !obj.get("source").isJsonNull) obj.get("source").asString else ""
        //     source.equals("Yahoo", ignoreCase = true)
        // }
        // 2. COMENTEAZĂ SAU REDU FILTRUL DE LUNGIME LA 0
        // .filter { obj ->
        //     val summary = if (obj.has("summary") && !obj.get("summary").isJsonNull) obj.get("summary").asString else ""
        //     summary.length > 500
        // }
        .map { obj ->
            val url = if (obj.has("url") && !obj.get("url").isJsonNull) obj.get("url").asString else "N/A"
            val datetime = if (obj.has("datetime") && !obj.get("datetime").isJsonNull) obj.get("datetime").asString else "N/A"
            val headline = if (obj.has("headline") && !obj.get("headline").isJsonNull) obj.get("headline").asString else "N/A"

            "URL: $url\nData: $datetime\nTitlu: $headline\n-------------------"
        }

    processed.print()

// RECOMANDARE: Asigură-te că ai exact aceste două linii la finalul main()-ului:
    sparkContext.start()
    sparkContext.awaitTermination()
}