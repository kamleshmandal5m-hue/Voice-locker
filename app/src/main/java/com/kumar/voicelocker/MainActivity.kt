package com.kumar.voicelocker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {
 private var recognizer: SpeechRecognizer? = null
 private lateinit var status: TextView
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(32,32,32,32) }
  status=TextView(this).apply { text="Voice Locker\n\n1. Enable Accessibility Service\n2. Tap Start Listening\n3. Say: Lock my phone"; textSize=20f; gravity=Gravity.CENTER }
  val settings=Button(this).apply { text="Enable Accessibility"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } }
  val start=Button(this).apply { text="Start Listening"; setOnClickListener { startListening() } }
  val stop=Button(this).apply { text="Stop Listening"; setOnClickListener { recognizer?.destroy(); recognizer=null; status.text="Listening stopped" } }
  layout.addView(status); layout.addView(settings); layout.addView(start); layout.addView(stop); setContentView(layout)
 }
 private fun startListening() {
  if (!SpeechRecognizer.isRecognitionAvailable(this)) { Toast.makeText(this,"Speech recognition unavailable",Toast.LENGTH_LONG).show(); return }
  recognizer?.destroy(); recognizer=SpeechRecognizer.createSpeechRecognizer(this)
  recognizer?.setRecognitionListener(object: RecognitionListener {
   override fun onReadyForSpeech(params: Bundle?) { status.text="Listening… Say: Lock my phone" }
   override fun onBeginningOfSpeech() {}
   override fun onRmsChanged(rmsdB: Float) {}
   override fun onBufferReceived(buffer: ByteArray?) {}
   override fun onEndOfSpeech() {}
   override fun onError(error: Int) { status.postDelayed({ listenAgain() },700) }
   override fun onResults(results: Bundle?) { val words=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.lowercase(Locale.ROOT).orEmpty()
    if (words.contains("lock my phone") || words.contains("phone lock") || words.contains("lock the phone")) {
     val service=LockAccessibilityService.instance
     if (service!=null) { status.text="Lock command detected"; service.lockPhone() } else { status.text="Please enable Voice Locker in Accessibility settings"; Toast.makeText(this@MainActivity,"Enable Accessibility first",Toast.LENGTH_LONG).show() }
    } else status.text="Heard: $words\nSay: Lock my phone"
    status.postDelayed({ listenAgain() },1000)
   }
   override fun onPartialResults(partialResults: Bundle?) {}
   override fun onEvent(eventType: Int, params: Bundle?) {}
  }); listenAgain()
 }
 private fun listenAgain() { val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault()); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false) }; try { recognizer?.startListening(intent) } catch (_: Exception) {} }
 override fun onDestroy() { recognizer?.destroy(); super.onDestroy() }
}
