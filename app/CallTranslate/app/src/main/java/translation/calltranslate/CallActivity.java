package translation.calltranslate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CallActivity extends AppCompatActivity implements RecognitionListener {

    private Context context;
    private SharedPreferences prefs;
    public static final String TAG = CallActivity.class.getSimpleName();
    private final OkHttpClient client = new OkHttpClient();
    private String microsoftAuthUrl = "https://datamarket.accesscontrol.windows.net/v2/OAuth2-13";
    private String myNumber;
    private String otherNumber;
    private String myLanguage;
    private String otherLanguage;
    private VoiceSynthesizer tts;
    private SpeechRecognizer mSpeechRecognizer = null;
    private Intent mSpeechRecognizerIntent;
    private boolean mIsListening;
    private ArrayList<Integer> voiceLevelChanges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        context = this;
        prefs = this.getSharedPreferences("translation.calltranslate", MODE_PRIVATE);

        myNumber = prefs.getString("phoneNumber", "None");
        Bundle b = getIntent().getExtras();
        if (b != null) {
            otherNumber = b.getString("otherNumber");
        } else {
            otherNumber = "None";
        }
        myLanguage = Locale.getDefault().getLanguage();
        otherLanguage = "ar";

        tts = new VoiceSynthesizer(context);
        setupSpeechInput();
        listen();

        FirebaseChat chat = new FirebaseChat(otherNumber, context, new FirebaseChat.OnNewMessageListener() {
            @Override
            public void onNewMessage(DataSnapshot dataSnapshot) {
                Log.d(TAG, "MESSAGE RECEIVED");
                String text = (String) dataSnapshot.child("text").getValue();
                Log.d(TAG, text);
//                sayMessage(text);
            }
        });
//        chat.send_message("Hola, ¿qué tal?");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        tts.finish();
    }

    private void getTranslation(final String message) throws Exception {
        RequestBody formBody = new FormBody.Builder()
                .add("client_id", "PennApps")
                .add("client_secret", "yR8VRcs+MsUPiqt7ee9IipEcoy03Bs35mvZbSGFtZ2o=")
                .add("scope", "http://api.microsofttranslator.com")
                .add("grant_type", "client_credentials")
                .build();
        Request request = new Request.Builder()
                .url(microsoftAuthUrl)
                .post(formBody)
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Request was successful
                    try {
                        JSONObject responseObj = new JSONObject(response.body().string());
                        String accessToken = responseObj.getString("access_token");
                        System.out.println("Access token: " + accessToken);

                        String uri = Uri.parse("http://api.microsofttranslator.com/v2/Http.svc/Translate?")
                                .buildUpon()
                                .appendQueryParameter("text", message)
                                .appendQueryParameter("from", myLanguage)
                                .appendQueryParameter("to", otherLanguage)
                                .build().toString();

                        String header = "Bearer " + accessToken;

                        Request request = new Request.Builder()
                                .url(uri)
                                .header("Authorization", header)
                                .build();

                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException throwable) {
                                throwable.printStackTrace();
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                if (!response.isSuccessful())
                                    throw new IOException("Unexpected code " + response);

                                System.out.println(response.body().string());
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Request not successful
                    throw new IOException("Unexpected code " + response);
                }
            }
        });
    }

    private void sayMessage(String message) {
        tts.speak(message);
    }

    protected void setupSpeechInput() {
        voiceLevelChanges = new ArrayList<>();
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(this);
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        if (!mSpeechRecognizerIntent.hasExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE)) {
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        }
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().getLanguage());
        mIsListening = false;
    }

    private void listen() {
        if (!mIsListening) {
            System.out.println("Started listening");
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
            voiceLevelChanges.clear();
            voiceLevelChanges.addAll(Arrays.asList(90, 90, 90, 90, 90));
//            recordCircle.setImageResource(R.drawable.record_circle_active);
        } else {
            mSpeechRecognizer.stopListening();
        }
        mIsListening = !mIsListening;
    }

    /**
     * Methods for RecognitionListener
     */
    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginingOfSpeech");
//        micText.setText(getString(R.string.now_recording));
//        recordCircle.getLayoutParams().width = 90;
//        recordCircle.getLayoutParams().height = 90;
//        recordCircle.requestLayout();
//        recordCircle.setImageResource(R.drawable.record_circle_active);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech");
//        micText.setText(getString(R.string.done_recording));
//        recordCircle.getLayoutParams().width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
//        recordCircle.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
//        recordCircle.requestLayout();
//        recordCircle.setImageResource(R.drawable.record_circle_inactive);
    }

    @Override
    public void onError(int error) {
        String mError = "";
        switch (error) {
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                mError = "Network timeout";
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                mError = "Network error";
                mSpeechRecognizer.cancel();
                mIsListening = false;
//                recordCircle.setImageResource(R.drawable.record_circle_inactive);
                break;
            case SpeechRecognizer.ERROR_AUDIO:
                mError = "Audio error";
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                break;
            case SpeechRecognizer.ERROR_SERVER:
                mError = "Server error";
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                mError = "Client error";
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                mError = "Speech timed out";
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                mError = "No match";
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                mError = "Speech recognition busy";
                mSpeechRecognizer.cancel();
                mIsListening = false;
//                recordCircle.setImageResource(R.drawable.record_circle_inactive);
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                mError = "Insufficient permissions";
                mSpeechRecognizer.cancel();
                mIsListening = false;
//                recordCircle.setImageResource(R.drawable.record_circle_inactive);
                break;
        }
        Log.i(TAG,  "Error: " +  error + " - " + mError);

//        micText.setText(mError);
//        recordCircle.getLayoutParams().width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
//        recordCircle.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
//        recordCircle.requestLayout();
    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        mIsListening = false;
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "onReadyForSpeech"); //$NON-NLS-1$
//        micText.setText(getString(R.string.begin_recording));
//        recordCircle.getLayoutParams().width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
//        recordCircle.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
//        recordCircle.requestLayout();
    }

    @Override
    public void onResults(Bundle results) {
        mIsListening = false;
//        micText.setText(getString(R.string.tap_on_mic));
//        recordCircle.getLayoutParams().width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
//        recordCircle.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
//        recordCircle.requestLayout();
//        recordCircle.setImageResource(R.drawable.record_circle_inactive);
        // Log.d(TAG, "onResults"); //$NON-NLS-1$
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        // matches are the return values of speech recognition engine
        if (matches != null) {
            // Log.d(TAG, matches.toString()); //$NON-NLS-1$
//            callApi(matches.get(0));
            System.out.println(matches.get(0));
            try {
                getTranslation(matches.get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Sorry, we couldn't understand you.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        if (rmsdB < 0) {
            rmsdB = 0;
        }
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (rmsdB * 8) + 80, getResources().getDisplayMetrics());

        voiceLevelChanges.remove(0);
        voiceLevelChanges.add(size);

        int adjustedSize = 0;

        for (int i = 0; i < voiceLevelChanges.size(); i++) {
            adjustedSize += voiceLevelChanges.get(i);
        }

        adjustedSize = adjustedSize / voiceLevelChanges.size();

//        if (!mIsListening) {
//            recordCircle.getLayoutParams().width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
//            recordCircle.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
//            recordCircle.setImageResource(R.drawable.record_circle_inactive);
//        } else {
//            recordCircle.getLayoutParams().width = adjustedSize;
//            recordCircle.getLayoutParams().height = adjustedSize;
//        }
//        recordCircle.requestLayout();
    }
}
