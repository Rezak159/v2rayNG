package com.v2ray.ang.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.compose.runtime.Composable
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import java.net.URLDecoder

/**
 * Entry point for subscription links: shared text, the `v2rayng://` scheme and the
 * a4vpn sign-in link from the Telegram bot.
 *
 * It only resolves the url and hands it to [MainActivity]. Importing here would not
 * survive — this activity finishes right away, which cancels any work started in its
 * own scope.
 */
class UrlSchemeActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val next = Intent(this, MainActivity::class.java)
        try {
            resolveSubscriptionUrl()?.let { url ->
                next.putExtra(MainActivity.EXTRA_SUBSCRIPTION_URL, url)
                next.putExtra(MainActivity.EXTRA_REPLACE_SUBSCRIPTION, isBotSignInLink())
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Error processing URL scheme", e)
        }

        startActivity(next)
        finish()
    }

    @Composable
    override fun ScreenContent() {
    }

    private fun isBotSignInLink(): Boolean =
        intent.action == Intent.ACTION_VIEW && intent.data?.host == AppConfig.APP_LINK_HOST

    /** Subscription url carried by the intent, or null if there is nothing to import. */
    private fun resolveSubscriptionUrl(): String? = when {
        intent.action == Intent.ACTION_SEND && intent.type == "text/plain" ->
            normalize(intent.getStringExtra(Intent.EXTRA_TEXT), null)

        intent.action == Intent.ACTION_VIEW -> when (intent.data?.host) {
            "install-config" -> {
                toastError(R.string.toast_action_not_allowed)
                null
            }

            "install-sub" -> normalize(
                intent.data?.getQueryParameter("url"),
                intent.data?.fragment,
            )

            AppConfig.APP_LINK_HOST -> normalize(decodeAppLink(intent.data), null)

            else -> {
                toastError(R.string.toast_failure)
                null
            }
        }

        else -> null
    }

    /**
     * Pulls the subscription url out of a bot sign-in link
     * (`https://<host>/app/sub/<base64url>`), or null if it is malformed.
     */
    private fun decodeAppLink(data: Uri?): String? {
        val payload = data?.path
            ?.removePrefix(AppConfig.APP_LINK_SUB_PATH)
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return try {
            val flags = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            String(Base64.decode(payload, flags), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            LogUtil.e(AppConfig.TAG, "Malformed app link payload", e)
            null
        }
    }

    /** Decodes the url, appends the remark carried as a fragment and validates it. */
    private fun normalize(uriString: String?, fragment: String?): String? {
        if (uriString.isNullOrEmpty()) return null

        var decoded = URLDecoder.decode(uriString, "UTF-8")
        if (!Utils.isValidSubUrl(decoded)) {
            toastError(R.string.toast_failure)
            return null
        }
        if (Uri.parse(decoded).fragment.isNullOrEmpty() && !fragment.isNullOrEmpty()) {
            decoded += "#${fragment}"
        }
        LogUtil.i(AppConfig.TAG, decoded)
        return decoded
    }
}
