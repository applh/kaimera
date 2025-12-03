package com.example.kaimera.core.ui

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

/**
 * Base activity class providing common functionality for all activities.
 */
abstract class BaseActivity : AppCompatActivity() {
    
    /**
     * Show a short toast message.
     */
    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show a long toast message.
     */
    protected fun showToastLong(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Show a snackbar message.
     */
    protected fun showSnackbar(message: String) {
        findViewById<android.view.View>(android.R.id.content)?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show a snackbar with an action.
     */
    protected fun showSnackbarWithAction(
        message: String,
        actionText: String,
        action: () -> Unit
    ) {
        findViewById<android.view.View>(android.R.id.content)?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction(actionText) { action() }
                .show()
        }
    }
}
