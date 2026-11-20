package com.example

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.viewmodel.OneHandViewModel
import org.lsposed.hiddenapibypass.HiddenApiBypass

/** Holds ONE ViewModel for the whole process so the app screen and the overlay show the same state. */
class OneHandApplication : Application(), ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    val viewModel: OneHandViewModel by lazy {
        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this))[OneHandViewModel::class.java]
    }

    override fun onCreate() {
        super.onCreate()
        // Android hides the window calls floating windows need. This lifts that for this app.
        bypassState = try {
            if (HiddenApiBypass.addHiddenApiExemptions("L")) "ok" else "returned false"
        } catch (t: Throwable) {
            "failed: " + t.javaClass.simpleName
        }
    }

    companion object {
        @Volatile var bypassState: String = "not run"
    }
}
