package com.jeanwest.reader

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class FindingProductAPI : Thread() {

    var response: String = ""
    var status = false

    @Volatile
    var run = false
    var similar: JSONArray = JSONArray()
    var stop = false
    var barcode: String = ""

    override fun run() {
        while (!stop) {
            if (run) {
                status = false
                try {
                    val urlString =
                        "http://rfid-api-0-1.avakatan.ir/products/similars?DepartmentInfo_ID=68&$barcode"
                    val server = URL(urlString)
                    val connection = server.openConnection() as HttpURLConnection
                    if (connection.responseCode == 200) {
                        val input: InputStream = BufferedInputStream(connection.inputStream)
                        val isr = InputStreamReader(input, "UTF-8")
                        val reader = BufferedReader(isr)
                        val receive = reader.readLine()
                        val json = JSONObject(receive)
                        similar = json.getJSONArray("products")
                        status = true
                    } else {
                        response =
                            connection.responseCode.toString() + " error: " + connection.responseMessage
                        WarehouseScanningActivity.databaseInProgress = false
                    }
                    connection.disconnect()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                run = false
            }
        }
    }
}