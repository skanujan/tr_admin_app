@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trelegantboutique.admin.ui.shell

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.JsonObject
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.ApiResult
import com.trelegantboutique.admin.data.str
import com.trelegantboutique.admin.ui.common.LocalDrawerIsOpen
import com.trelegantboutique.admin.ui.common.PageScaffold
import com.trelegantboutique.admin.ui.theme.DeepGreen
import com.trelegantboutique.admin.ui.theme.Gold
import kotlinx.coroutines.launch
import java.net.URLEncoder

/** Plain holder for things the WebView callbacks change (not shown on screen). */
private class WebHolder {
    var view: WebView? = null
    var fileCallback: ValueCallback<Array<Uri>>? = null
    var reloginTried = false
}

/** Lets a page's own "Print" button (window.print) open the phone's print screen. */
private class PrintBridge(private val context: Context, private val holder: WebHolder) {
    @JavascriptInterface
    fun print() {
        val v = holder.view ?: return
        v.post {
            try {
                val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val name = (v.title ?: "Admin page").take(40)
                pm.print(name, v.createPrintDocumentAdapter(name), PrintAttributes.Builder().build())
            } catch (e: Exception) {
                Toast.makeText(context, "Printing is not available on this phone.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * Shows one website admin page inside the app, already signed in as the
 * app's user (through a one-time code, see api/web-code.php). Used for every
 * menu item that does not have its own app screen, so the app has every
 * feature of the website admin.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPageScreen(
    repo: ApiRepository,
    siteHost: String,
    path: String,
    title: String,
    showBack: Boolean,
    onExit: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val holder = remember { WebHolder() }
    var progress by remember { mutableStateOf(0) }
    var pageTitle by remember { mutableStateOf(title) }
    var error by remember { mutableStateOf<String?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val cb = holder.fileCallback
        holder.fileCallback = null
        var uris: Array<Uri>? = WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data)
        val clip = res.data?.clipData
        if (clip != null && clip.itemCount > 1) {
            uris = Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
        }
        cb?.onReceiveValue(uris)
    }

    fun openSignedIn(target: String) {
        error = null
        scope.launch {
            when (val r = repo.postJson("web-code.php", JsonObject())) {
                is ApiResult.Success -> {
                    val loginUrl = r.data.str("login_url")
                    val code = r.data.str("code")
                    if (loginUrl.isBlank() || code.isBlank()) {
                        error = "Please upload website update 222, then try again."
                    } else {
                        val url = loginUrl + "?c=" + code + "&to=" + URLEncoder.encode(target, "UTF-8")
                        holder.view?.loadUrl(url)
                    }
                }
                is ApiResult.Failure -> {
                    if (r.unauthorized) { onSessionExpired() } else { error = r.message }
                }
            }
        }
    }

    BackHandler(enabled = !LocalDrawerIsOpen.current) {
        val v = holder.view
        if (v != null && v.canGoBack()) v.goBack() else onExit()
    }

    DisposableEffect(Unit) {
        onDispose {
            holder.fileCallback?.onReceiveValue(null)
            holder.fileCallback = null
            holder.view?.destroy()
            holder.view = null
        }
    }

    PageScaffold(
        title = pageTitle,
        onBack = if (showBack || canGoBack) ({
            val v = holder.view
            if (v != null && v.canGoBack()) v.goBack() else onExit()
        }) else null,
        snackbar = snackbar,
        actions = {
            IconButton(onClick = { holder.view?.reload() }) { Icon(Icons.Filled.Refresh, contentDescription = "Reload") }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        holder.view = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.allowFileAccess = false
                        settings.setSupportMultipleWindows(false)
                        settings.userAgentString = settings.userAgentString + " TRAdminApp/1.2"
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                        addJavascriptInterface(PrintBridge(ctx, holder), "TRApp")

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val uri = request.url
                                val scheme = uri.scheme ?: ""
                                val sameSite = (scheme == "https" || scheme == "http") && uri.host == siteHost
                                if (sameSite) {
                                    val p = uri.path ?: ""
                                    if (p.endsWith("/admin/login.php")) {
                                        // The website session ended: sign in again quietly, once.
                                        if (!holder.reloginTried) {
                                            holder.reloginTried = true
                                            openSignedIn(path)
                                            return true
                                        }
                                    }
                                    return false
                                }
                                return try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    true
                                } catch (e: Exception) {
                                    true
                                }
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                canGoBack = view.canGoBack()
                                // Make the page's Print buttons use the phone's print screen.
                                view.evaluateJavascript(
                                    "window.print=function(){try{TRApp.print();}catch(e){}};",
                                    null
                                )
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }

                            override fun onReceivedTitle(view: WebView, t: String?) {
                                val clean = (t ?: "").substringBefore(" | ").trim()
                                if (clean.isNotEmpty() && !clean.startsWith("http")) pageTitle = clean
                            }

                            override fun onShowFileChooser(
                                webView: WebView,
                                filePathCallback: ValueCallback<Array<Uri>>,
                                fileChooserParams: FileChooserParams
                            ): Boolean {
                                holder.fileCallback?.onReceiveValue(null)
                                holder.fileCallback = filePathCallback
                                return try {
                                    chooser.launch(fileChooserParams.createIntent())
                                    true
                                } catch (e: Exception) {
                                    holder.fileCallback = null
                                    filePathCallback.onReceiveValue(null)
                                    false
                                }
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                            try {
                                val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
                                val req = DownloadManager.Request(Uri.parse(url))
                                val cookie = CookieManager.getInstance().getCookie(url)
                                if (cookie != null) req.addRequestHeader("Cookie", cookie)
                                req.addRequestHeader("User-Agent", userAgent)
                                req.setTitle(name)
                                req.setMimeType(mimeType)
                                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                dm.enqueue(req)
                                Toast.makeText(ctx, "Downloading $name (see your notifications)", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Could not download this file.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    color = Gold,
                    trackColor = DeepGreen,
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                )
            }
            val e = error
            if (e != null) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(e)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { openSignedIn(path) }) { Text("Try again") }
                }
            }
        }
    }

    LaunchedEffect(path) {
        holder.reloginTried = false
        openSignedIn(path)
    }
}
