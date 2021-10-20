package dev.starcat.sms2api

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

object Const {

    const val SHARED_PREF_URL_KEY = "url"
    const val SHARED_PREF_URL_DEFAULT_VALUE = ""

    const val SHARED_PREF_API_KEY_KEY = "apiKey"
    const val SHARED_PREF_API_KEY_DEFAULT_VALUE = ""

    const val SHARED_PREF_USER_DEFINED_ID_KEY = "userDefinedId"
    const val SHARED_PREF_USER_DEFINED_ID_VALUE = ""

    // Constants used in Store
    const val MIN_BASE_URL_LENGTH = 11
}

class MainActivity : AppCompatActivity(), StoreListener, TextWatcher {

    private val tag = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Store.addListener(this)

        checkPermissions()

        initUI()
        updateUI()
    }

    override fun onDestroy() {

        super.onDestroy()
        Store.removeListener(this)

        Log.d(tag, "Main activity finished.")
    }

    private fun getStoredValue(key: String, defaultValue: String): Editable {

        val sharedPref = this@MainActivity.getPreferences(Context.MODE_PRIVATE)
        val value = sharedPref.getString(key, defaultValue)

        Log.d(tag, "Value for '$key' key is '$value'")

        return Editable.Factory.getInstance().newEditable(value)
    }

    private fun setStoredValue(key: String, value: String) {

        val sharedPref = this@MainActivity.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(key, value)
        editor.apply()

        Log.d(tag, "Value '$value' for '$key' key has been stored.")
    }


    private fun initUI() {

        val urlValue = findViewById<EditText>(R.id.apiUrl)
        val userDefinedId = findViewById<EditText>(R.id.userDefinedId)
        val apiKey = findViewById<EditText>(R.id.apiKey)

        val editButton = findViewById<Button>(R.id.edit)

        editButton.setOnClickListener {
            urlValue.isEnabled = !urlValue.isEnabled
            userDefinedId.isEnabled = !userDefinedId.isEnabled
            apiKey.isEnabled = !apiKey.isEnabled
            if (urlValue.isEnabled) editButton.text = "Lock" else editButton.text = "Edit"
        }

        //We have to initialize the listener first invoke the callback when we set the value read from the shared preferences
        //the store's URL will be set inside the callback!
        urlValue.addTextChangedListener(this)
        userDefinedId.addTextChangedListener(this)
        apiKey.addTextChangedListener(this)

        urlValue.text = getStoredValue(
            Const.SHARED_PREF_URL_KEY,
            Const.SHARED_PREF_URL_DEFAULT_VALUE
        )

        userDefinedId.text = getStoredValue(
            Const.SHARED_PREF_USER_DEFINED_ID_KEY,
            Const.SHARED_PREF_USER_DEFINED_ID_VALUE
        )

        apiKey.text = getStoredValue(
            Const.SHARED_PREF_API_KEY_KEY,
            Const.SHARED_PREF_API_KEY_DEFAULT_VALUE
        )

        val startDateTime = findViewById<TextView>(R.id.applicationStartDateTime)
        startDateTime.text = SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(Date())
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {

        val received = findViewById<TextView>(R.id.receivedCount)
        received.text = "${Store.receivedCount}"

        val forwarded = findViewById<TextView>(R.id.forwardedCount)
        forwarded.text =  "${Store.forwardedCount}"

        val errored = findViewById<TextView>(R.id.erroredCount)
        errored.text = "${Store.failedCount}"

        val errorMessage = findViewById<TextView>(R.id.errorMessage)
        if (Store.failedCount > 0 && errorMessage.visibility == INVISIBLE) {

            val erroredLabel = findViewById<TextView>(R.id.failedLabel)

            errored.visibility = VISIBLE
            erroredLabel.visibility = VISIBLE
            errorMessage.visibility = VISIBLE
        }
    }

    private fun checkPermissions() {

        if ( ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.RECEIVE_SMS
                )) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_SMS
                        ), 1
                    )
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.RECEIVE_SMS),
                    1
                )
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECEIVE_SMS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.d(tag, "${Manifest.permission.RECEIVE_SMS} Permission Granted")
                    }
                } else {
                    Log.d(tag, "${Manifest.permission.RECEIVE_SMS} Permission Denied")
                }
                return
            }
        }
    }

    override fun onCounterChange() {
        updateUI()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // do nothing
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // do nothing
    }

    override fun afterTextChanged(s: Editable?) {

        val urlValue = findViewById<EditText>(R.id.apiUrl)
        val uniqueIdValue = findViewById<EditText>(R.id.userDefinedId)
        val apiKey = findViewById<EditText>(R.id.apiKey)

        if (s != null) {

            val str = s.toString()
            when {
                s === urlValue.editableText -> {
                    Store.url = str
                    setStoredValue(Const.SHARED_PREF_URL_KEY, str)
                }
                s === uniqueIdValue.editableText -> {
                    Store.userDefinedId = str
                    setStoredValue(Const.SHARED_PREF_USER_DEFINED_ID_KEY, str)
                }
                s === apiKey.editableText -> {
                    Store.apiKey = str
                    setStoredValue(Const.SHARED_PREF_API_KEY_KEY, str)
                }
            }

        }
    }

}
