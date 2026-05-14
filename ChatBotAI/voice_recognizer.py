import speech_recognition as sr
import sys

def recognize_voice():
    r = sr.Recognizer()
    with sr.Microphone() as source:
        # Adjust for ambient noise
        r.adjust_for_ambient_noise(source, duration=0.5)
        try:
            # Listening
            audio = r.listen(source, timeout=5, phrase_time_limit=5)
            # Using google speech recognition
            text = r.recognize_google(audio, language='fr-FR')
            return text
        except sr.WaitTimeoutError:
            return "TIMEOUT"
        except sr.UnknownValueError:
            return "UNKNOWN"
        except Exception as e:
            return f"ERROR: {str(e)}"

if __name__ == "__main__":
    try:
        import speech_recognition as sr
        result = recognize_voice()
        print(result)
    except ImportError:
        print("ERROR: Bibliothèques manquantes. Installez SpeechRecognition et PyAudio.")
    except Exception as e:
        print(f"ERROR: {str(e)}")
