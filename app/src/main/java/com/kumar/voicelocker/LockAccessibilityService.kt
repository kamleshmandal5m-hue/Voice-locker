package com.kumar.voicelocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class LockAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onServiceConnected() { instance = this }
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }
    companion object { @Volatile var instance: LockAccessibilityService? = null }
    fun lockPhone() { performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) }
}
