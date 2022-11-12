package com.geeklabs.webviewkotlin

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var progress: Progress? = null
    private var isLoaded: Boolean = false
    private var doubleBackToExitPressedOnce = false
    private var webURL = "https://https://www.kedaiemasperling.com.my/"
    //declaration of FAB
    private lateinit var plusFAB: FloatingActionButton
    private lateinit var emailFAB: FloatingActionButton
    private lateinit var locationFAB: FloatingActionButton
    private lateinit var callFAB: FloatingActionButton
    private var fabVisible = false

    @SuppressLint("SetJavaScriptEnabled", "IntentReset")
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //calling toolbar
        // setSupportActionBar(findViewById(R.id.my_toolbar))
        // setSupportActionBar(my_toolbar)
        title = "Kedai Emas Perling"
        val settings = webView.settings

        setDesktopMode(webView, true)
        plusFAB = findViewById(R.id.idFABPlus)
        emailFAB = findViewById(R.id.idFABEmail)
        locationFAB = findViewById(R.id.idFABLocation)
        callFAB = findViewById(R.id.idFABCall)
        emailFAB.visibility = View.GONE
        locationFAB.visibility = View.GONE
        callFAB.visibility = View.GONE
        webView.settings.builtInZoomControls = true
        webView.settings.setSupportZoom(true)
        webView.settings.displayZoomControls = false

        plusFAB.setOnClickListener {
            if (!fabVisible) {
                emailFAB.show()
                locationFAB.show()
                callFAB.show()
                emailFAB.visibility = View.VISIBLE
                locationFAB.visibility = View.VISIBLE
                callFAB.visibility = View.VISIBLE
                plusFAB.setImageDrawable(getDrawable(android.R.drawable.ic_delete))
                fabVisible = true
            } else {
                emailFAB.hide()
                locationFAB.hide()
                callFAB.hide()
                emailFAB.visibility = View.GONE
                locationFAB.visibility = View.GONE
                callFAB.visibility = View.GONE
                plusFAB.setImageDrawable(getDrawable(android.R.drawable.ic_input_add))
                fabVisible = false
            }
        }
        emailFAB.setOnClickListener {
            //sent email function will be here
            /*ACTION_SEND action to launch an email client installed on your Android device.*/
            val mIntent = Intent(Intent.ACTION_SENDTO)
            /*To send an email you need to specify mailto: as URI using setData() method
            and data type will be to text/plain using setType() method*/
            mIntent.data = Uri.parse("mailto:")
            mIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("website.kedaiemasperling@gmail.com"))
            mIntent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry")
            mIntent.putExtra(Intent.EXTRA_TEXT, "Saya ingin mengetahui...")
            // put recipient, subject and email body in intent

            try {
                //start email intent
                startActivity(Intent.createChooser(mIntent, "Choose Email Client..."))
            } catch (e: Exception) {
                //if any thing goes wrong for example no email client application or any exception
                //get and show exception message
                Toast.makeText(this, e.message, LENGTH_LONG).show()
            }
            Toast.makeText(this@MainActivity, "Sending Email. . . ", LENGTH_LONG).show()
        }
        locationFAB.setOnClickListener {
            //location function will be here
            try {
                // Launch Waze to look for kedaiemasperling:
                val url =
                    "https://waze.com/ul?ll=1.4951455514835994, 103.68039796893288&navigate=yes"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                // If Waze is not installed, open it in Google Play:
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.waze"))
                startActivity(intent)
            }
            Toast.makeText(this@MainActivity, "Opening Waze app to Kedai Emas Perling. . .", LENGTH_LONG).show()
        }
        callFAB.setOnClickListener {
            //redirect to phone app and send the phone number
            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.data = Uri.parse("tel:" + "072075335")
            startActivity(dialIntent)
            Toast.makeText(this@MainActivity, "Opening phone app. . .", LENGTH_LONG).show()
        }

        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.domStorageEnabled = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.supportMultipleWindows()
        progress = Progress(this, R.string.please_wait, cancelable = true)

        //checking if the phone have internet connection
        if (!isOnline()) {
            showToast(getString(R.string.no_internet))
            infoTV.text = getString(R.string.no_internet)
            showNoNetSnackBar()
            return
        }
    }

    fun setDesktopMode(webView: WebView, enabled: Boolean) {
        var newUserAgent: String? = webView.settings.userAgentString
        if (enabled) {
            try {
                val ua: String = webView.settings.userAgentString
                val androidOSString: String = webView.settings.userAgentString.substring(
                    ua.indexOf("("),
                    ua.indexOf(")") + 1
                )
                newUserAgent = webView.settings.userAgentString.replace(androidOSString, "(X11; chrome x86_64)")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            newUserAgent = null
        }
        webView.settings.apply {
            userAgentString = newUserAgent
            useWideViewPort = enabled
            loadWithOverviewMode = enabled
        }
        webView.reload()
    }

    override fun onResume() {
        if (isOnline() && !isLoaded) loadWebView()
        super.onResume()
    }

    private fun loadWebView() {
        showProgress(true)
        infoTV.text = ""
        webView.loadUrl(webURL)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("tel:") || url.startsWith("whatsapp:")) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    webView.goBack()
                    return true
                } else {
                    view?.loadUrl(url)
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                showProgress(true)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isLoaded = true
                showProgress(false)
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError) {
                isLoaded = false
                val errorMessage = "Got Error! $error"
                showToast(errorMessage)
                infoTV.text = errorMessage
                showProgress(false)
                super.onReceivedError(view, request, error)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    showToastToExit()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showToastToExit() {
        when {
            doubleBackToExitPressedOnce -> {
                onBackPressed()
            }
            else -> {
                doubleBackToExitPressedOnce = true
                showToast(getString(R.string.back_again_to_exit))
                Handler(Looper.myLooper()!!).postDelayed(
                    { doubleBackToExitPressedOnce = false },
                    2000
                )
            }
        }
    }

    private fun showProgress(visible: Boolean) {
        progress?.apply { if (visible) show() else dismiss() }
    }

    private fun isOnline(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showNoNetSnackBar() {
        val snack =
            Snackbar.make(rootView, getString(R.string.no_internet), Snackbar.LENGTH_INDEFINITE)
        snack.setAction(getString(R.string.settings)) {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        snack.show()
    }
}