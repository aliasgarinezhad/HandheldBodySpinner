package com.jeanwest.reader.checkIn

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import coil.annotation.ExperimentalCoilApi
import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeanwest.reader.sharedClassesAndFiles.ExceptionHandler
import com.jeanwest.reader.sharedClassesAndFiles.JalaliDate.JalaliDate
import com.jeanwest.reader.MainActivity
import com.jeanwest.reader.R
import com.jeanwest.reader.sharedClassesAndFiles.Product
import com.jeanwest.reader.sharedClassesAndFiles.ErrorSnackBar
import com.jeanwest.reader.sharedClassesAndFiles.Item
import com.jeanwest.reader.sharedClassesAndFiles.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream


@OptIn(ExperimentalFoundationApi::class)
class ConfirmCheckInsActivity : ComponentActivity() {

    private var conflictResultProducts = mutableStateListOf<Product>()
    private var uiList = mutableStateListOf<Product>()
    private var openDialog by mutableStateOf(false)
    private var fileName by mutableStateOf("?????????? ?????????? ?????? ?????????? ")
    private var shortagesNumber by mutableStateOf(0)
    private var additionalNumber by mutableStateOf(0)
    private var numberOfScanned by mutableStateOf(0)
    private var checkInProperties = mutableListOf<CheckInProperties>()
    private var state = SnackbarHostState()
    private lateinit var queue : RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Page()
        }

        queue = Volley.newRequestQueue(this)

        val type = object : TypeToken<SnapshotStateList<Product>>() {}.type

        conflictResultProducts = Gson().fromJson(
            intent.getStringExtra("additionalAndShortageProducts"), type
        ) ?: mutableStateListOf()

        uiList = conflictResultProducts.filter { it1 ->
            it1.scan == "????????" || it1.scan == "??????????" || it1.scan == "?????????? ????????"
        }.toMutableStateList()

        numberOfScanned = intent.getIntExtra("numberOfScanned", 0)
        shortagesNumber = intent.getIntExtra("shortagesNumber", 0)
        additionalNumber = intent.getIntExtra("additionalNumber", 0)

        loadMemory()

        val util = JalaliDate()
        fileName += util.currentShamsidate

        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(this, Thread.getDefaultUncaughtExceptionHandler()!!))
    }

    private fun loadMemory() {

        val type = object : TypeToken<List<CheckInProperties>>() {}.type

        val memory = PreferenceManager.getDefaultSharedPreferences(this)

        checkInProperties = Gson().fromJson(
            memory.getString("GetBarcodesByCheckInNumberActivityUiList", ""),
            type
        ) ?: mutableListOf()
    }

    private fun exportFile() {

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("??????????????")

        val headerRow = sheet.createRow(sheet.physicalNumberOfRows)
        headerRow.createCell(0).setCellValue("???? ?????? ?? ????")
        headerRow.createCell(1).setCellValue("??????????")
        headerRow.createCell(2).setCellValue("????????")
        headerRow.createCell(3).setCellValue("??????????")
        headerRow.createCell(4).setCellValue("??????????")

        conflictResultProducts.forEach {
            val row = sheet.createRow(sheet.physicalNumberOfRows)
            row.createCell(0).setCellValue(it.KBarCode)
            row.createCell(1).setCellValue(it.scannedNumber.toDouble())

            if (it.scan == "????????") {
                row.createCell(2).setCellValue(it.matchedNumber.toDouble())
            } else if (it.scan == "??????????" || it.scan == "?????????? ????????") {
                row.createCell(3).setCellValue(it.matchedNumber.toDouble())
            }
        }

        val row = sheet.createRow(sheet.physicalNumberOfRows)
        row.createCell(0).setCellValue("??????????")
        row.createCell(1).setCellValue(numberOfScanned.toDouble())
        row.createCell(2).setCellValue(shortagesNumber.toDouble())
        row.createCell(3).setCellValue(additionalNumber.toDouble())

        val sheet2 = workbook.createSheet("????????")

        val header2Row = sheet2.createRow(sheet2.physicalNumberOfRows)
        header2Row.createCell(0).setCellValue("???? ?????? ?? ????")
        header2Row.createCell(1).setCellValue("????????????")
        header2Row.createCell(2).setCellValue("????????")
        header2Row.createCell(3).setCellValue("??????????")

        conflictResultProducts.forEach {

            if (it.scan == "????????") {
                val shortageRow = sheet2.createRow(sheet2.physicalNumberOfRows)
                shortageRow.createCell(0).setCellValue(it.KBarCode)
                shortageRow.createCell(1)
                    .setCellValue(it.scannedNumber.toDouble() + it.matchedNumber.toDouble())
                shortageRow.createCell(2).setCellValue(it.matchedNumber.toDouble())
            }
        }

        val sheet3 = workbook.createSheet("??????????")

        val header3Row = sheet3.createRow(sheet3.physicalNumberOfRows)
        header3Row.createCell(0).setCellValue("???? ?????? ?? ????")
        header3Row.createCell(1).setCellValue("????????????")
        header3Row.createCell(2).setCellValue("??????????")
        header3Row.createCell(3).setCellValue("??????????")

        conflictResultProducts.forEach {

            if (it.scan == "??????????") {
                val additionalRow = sheet3.createRow(sheet3.physicalNumberOfRows)
                additionalRow.createCell(0).setCellValue(it.KBarCode)
                additionalRow.createCell(1)
                    .setCellValue(it.matchedNumber - it.scannedNumber.toDouble())
                additionalRow.createCell(2).setCellValue(it.matchedNumber.toDouble())
            }
        }

        val dir = File(this.getExternalFilesDir(null), "/")

        val outFile = File(dir, "$fileName.xlsx")

        val outputStream = FileOutputStream(outFile.absolutePath)
        workbook.write(outputStream)
        outputStream.flush()
        outputStream.close()

        val uri = FileProvider.getUriForFile(
            this,
            this.applicationContext.packageName + ".provider",
            outFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "application/octet-stream"
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(shareIntent)
    }

    private fun confirmCheckIns() {

        if (checkInProperties.size == 0) {

            CoroutineScope(Dispatchers.Default).launch {
                state.showSnackbar(
                    "?????????? ???? ???????? ?????????? ???????? ??????????",
                    null,
                    SnackbarDuration.Long
                )
            }
            return
        }

        val url = "https://rfid-api.avakatan.ir/stock-draft/confirm"
        val request = object : JsonArrayRequest(Method.POST, url, null, {

            CoroutineScope(Dispatchers.Default).launch {
                state.showSnackbar(
                    "?????????? ???? ???? ???????????? ?????????? ????????",
                    null,
                    SnackbarDuration.Long
                )
            }
        }, {
            if (it is NoConnectionError) {

                CoroutineScope(Dispatchers.Default).launch {
                    state.showSnackbar(
                        "?????????????? ?????? ??????. ???????? ?????? ?????? ???? ?????????? ????????.",
                        null,
                        SnackbarDuration.Long
                    )
                }
            } else {

                val error = JSONObject(it.networkResponse.data.decodeToString()).getJSONObject("error")

                CoroutineScope(Dispatchers.Default).launch {
                    state.showSnackbar(
                        error.getString("message"),
                        null,
                        SnackbarDuration.Long
                    )
                }
            }
        }) {
            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json;charset=UTF-8"
                params["Authorization"] = "Bearer " + MainActivity.token
                return params
            }

            override fun getBody(): ByteArray {
                val body = JSONArray()
                checkInProperties.forEach {
                    body.put(it.number)
                }
                return body.toString().toByteArray()
            }
        }
        queue.add(request)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == 4) {
            back()
        }
        return true
    }

    private fun back() {
        queue.stop()
        finish()
    }

    @ExperimentalCoilApi
    @ExperimentalFoundationApi
    @Composable
    fun Page() {
        MyApplicationTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Scaffold(
                    topBar = { AppBar() },
                    content = { Content() },
                    bottomBar = {
                        BottomAppBar()
                    },
                    snackbarHost = { ErrorSnackBar(state) },
                )
            }
        }
    }

    @Composable
    fun BottomAppBar() {
        BottomAppBar(backgroundColor = MaterialTheme.colors.background) {

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "?????? ????????: $shortagesNumber",
                    textAlign = TextAlign.Right,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )

                Text(
                    text = "?????? ??????????: $additionalNumber",
                    textAlign = TextAlign.Right,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )

                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                ) {
                    Button(
                        onClick = {
                            confirmCheckIns()
                        },
                    ) {
                        Text(text = "?????????? ?????????? ????")
                    }
                }
            }
        }
    }

    @Composable
    fun AppBar() {

        TopAppBar(

            navigationIcon = {
                IconButton(onClick = { finish() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = ""
                    )
                }
            },

            actions = {
                IconButton(onClick = { openDialog = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_share_24),
                        contentDescription = ""
                    )
                }
            },

            title = {
                Text(
                    text = stringResource(id = R.string.checkInText),
                    modifier = Modifier
                        .padding(end = 15.dp)
                        .fillMaxSize()
                        .wrapContentSize(),
                    textAlign = TextAlign.Center,
                )
            }
        )
    }

    @ExperimentalCoilApi
    @ExperimentalFoundationApi
    @Composable
    fun Content() {

        Column {

            if (openDialog) {
                FileAlertDialog()
            }

            LazyColumn(modifier = Modifier.padding(top = 8.dp, bottom = 56.dp)) {

                items(uiList.size) { i ->
                        Item(
                            i,
                            uiList,
                            text1 = "????????????: " + uiList[i].desiredNumber,
                            text2 = uiList[i].result
                        )
                }
            }
        }
    }

    @Composable
    fun FileAlertDialog() {

        AlertDialog(

            buttons = {

                Column {

                    Text(
                        text = "?????? ???????? ?????????? ???? ???????? ????????", modifier = Modifier
                            .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                    )

                    OutlinedTextField(
                        value = fileName, onValueChange = {
                            fileName = it
                        },
                        modifier = Modifier
                            .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Button(modifier = Modifier
                        .padding(bottom = 10.dp, top = 10.dp, start = 10.dp, end = 10.dp)
                        .align(Alignment.CenterHorizontally),
                        onClick = {
                            openDialog = false
                            exportFile()
                        }) {
                        Text(text = "??????????")
                    }
                }
            },

            onDismissRequest = {
                openDialog = false
            }
        )
    }
}