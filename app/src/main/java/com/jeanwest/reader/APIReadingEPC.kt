package com.jeanwest.reader

import kotlin.jvm.Volatile
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList

class APIReadingEPC : Thread() {

    @JvmField
    var response: String = ""

    @JvmField
    var status = false

    @Volatile
    @JvmField
    var run = true

    override fun run() {

        try {

            val getCommand =
                "http://rfid-api-0-1.avakatan.ir/stock-taking/" + WarehouseScanningActivity.ID + "/epcs/v2"
            val server = URL(getCommand)
            val connection = server.openConnection() as HttpURLConnection
            connection.doOutput = true
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doInput = true
            val out = OutputStreamWriter(connection.outputStream)
            val temp = ArrayList<String>()
            for ((key) in WarehouseScanningActivity.EPCTableValid) {
                temp.add('"' + key + '"')
            }
            out.write(temp.toString())
            out.close()
            val statusCode = connection.responseCode

            if (statusCode == 204) {
                status = true
            } else if (statusCode == 200) {
                status = true
            } else {
                response = connection.responseCode.toString() + " error: " + connection.responseMessage
                WarehouseScanningActivity.databaseInProgress = false
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        run = false
    }
}