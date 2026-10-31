package com.knoopbyknoop.android

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.knoopbyknoop.core.Language
import java.util.Locale

/**
 * Speaks the navigation's notices in the app's language, lowering other audio meanwhile, see
 * `knot-navigation` "Spoken notices" and `app-languages` "Spoken notices in the language".
 */
class Speaker(context: Context) {

  private val locale = context.resources.configuration.locales[0]

  /**
   * The language the notices are spoken in: the app's where the phone has a voice for it, else
   * English. The app's until the speech engine is ready.
   */
  @Volatile
  var language: Language = Language.of(locale.language)
    private set

  private val audioManager = context.getSystemService(AudioManager::class.java)
  private val attributes =
    AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
      .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
      .build()
  private val focusRequest =
    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
      .setAudioAttributes(attributes)
      .build()
  private var ready = false
  private val waiting = mutableListOf<String>()
  private val speaking = mutableSetOf<String>()
  private var onSilent: (() -> Unit)? = null
  private var nextId = 0

  private val speech: TextToSpeech =
    TextToSpeech(context) { status ->
      if (status != TextToSpeech.SUCCESS) return@TextToSpeech
      if (language != Language.EN && speech.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE)
        language = Language.EN
      if (language == Language.EN && speech.setLanguage(Locale.UK) < TextToSpeech.LANG_AVAILABLE)
        speech.setLanguage(Locale.ENGLISH)
      ready = true
      waiting.forEach(::speakNow)
      waiting.clear()
    }

  init {
    speech.setAudioAttributes(attributes)
    speech.setOnUtteranceProgressListener(
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {}

        override fun onDone(utteranceId: String) = finished(utteranceId)

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) = finished(utteranceId)

        override fun onError(utteranceId: String, errorCode: Int) = finished(utteranceId)
      }
    )
  }

  fun speak(text: String) {
    if (ready) speakNow(text) else waiting += text
  }

  /** Calls [action] once nothing is being spoken anymore, e.g. to stop after "You've arrived". */
  fun whenSilent(action: () -> Unit) {
    synchronized(speaking) {
      if (speaking.isEmpty() && waiting.isEmpty()) action() else onSilent = action
    }
  }

  fun shutdown() {
    speech.stop()
    speech.shutdown()
    audioManager.abandonAudioFocusRequest(focusRequest)
  }

  private fun speakNow(text: String) {
    val id = "notice-${nextId++}"
    synchronized(speaking) { speaking += id }
    audioManager.requestAudioFocus(focusRequest)
    speech.speak(text, TextToSpeech.QUEUE_ADD, null, id)
  }

  private fun finished(id: String) {
    val action =
      synchronized(speaking) {
        speaking -= id
        if (speaking.isNotEmpty()) return
        // Other audio comes back after each notice.
        audioManager.abandonAudioFocusRequest(focusRequest)
        onSilent.also { onSilent = null }
      }
    action?.invoke()
  }
}
