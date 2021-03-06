/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license.
 * //
 * Project Oxford: http://ProjectOxford.ai
 * //
 * ProjectOxford SDK GitHub:
 * https://github.com/Microsoft/ProjectOxford-ClientSDK
 * //
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 * //
 * MIT License:
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * //
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * //
 * THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.CognitiveServicesExample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import com.microsoft.bing.speech.AudioStream;
import com.microsoft.bing.speech.Conversation;
import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import android.os.StrictMode;
import android.widget.Toast;

import javax.net.ssl.HttpsURLConnection;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements ISpeechRecognitionServerEvents
{
    int m_waitSeconds = 0;
    DataRecognitionClient dataClient = null;
    MicrophoneRecognitionClient micClient = null;
    FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;
    EditText _logText;
    ImageButton _startButton;
    ImageButton _stopButton;
    boolean isTheEnd = false;
    int folderMemory = 0;
    int fileMemory = 0;
    boolean skipFeedback = true;
    private Object lock = new Object();



    public enum FinalResponseStatus { NotReceived, OK, Timeout }

    /**
     * Gets the primary subscription key
     */
    public String getPrimaryKey() {
        return this.getString(R.string.primaryKey);
    }

    /**
     * Gets the LUIS application identifier.
     * @return The LUIS application identifier.
     */
    private String getLuisAppId() {
        return this.getString(R.string.luisAppID);
    }

    /**
     * Gets the LUIS subscription identifier.
     * @return The LUIS subscription identifier.
     */
    private String getLuisSubscriptionID() {
        return this.getString(R.string.luisSubscriptionID);
    }



    /**
     * Gets the current speech recognition mode.
     * @return The speech recognition mode.
     */
    private SpeechRecognitionMode getMode() {
        return SpeechRecognitionMode.ShortPhrase;
    }

    /**
     * Gets the default locale.
     * @return The default locale.
     */
    private String getDefaultLocale() {
        return "nl-NL";
    }


    /**
     * Gets the Cognitive Service Authentication Uri.
     * @return The Cognitive Service Authentication Uri.  Empty if the global default is to be used.
     */
    private String getAuthenticationUri() {
        return this.getString(R.string.authenticationUri);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        this._logText = (EditText) findViewById(R.id.editText1);
        this._startButton = (ImageButton) findViewById(R.id.imageButton);
        this._stopButton = (ImageButton) findViewById(R.id.imageButton2);

        if (getString(R.string.primaryKey).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }

        // setup the buttons
        final MainActivity This = this;
        this._startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                This.StartButton_Click(arg0);
            }
        });

        this._stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
               System.exit(1);
            }
        });


        this.ShowMenu(true);
    }


    private void ShowMenu(boolean show) {
        if (show) {
            this._logText.setVisibility(View.INVISIBLE);
        } else {
            this._logText.setText("");
            this._logText.setVisibility(View.VISIBLE);
        }
    }
    /**
     * Handles the Click event of the _startButton control.
     */
    private void StartButton_Click(View arg0) {
        this._startButton.setEnabled(false);

        this.m_waitSeconds = this.getMode() == SpeechRecognitionMode.ShortPhrase ? 20 : 200;

        this.ShowMenu(false);

        this.LogRecognitionStart();

        startMicrophone();
    }

    /**
     * Logs the recognition start.
     */
    private void LogRecognitionStart() {
        String recoSource = "microphone";

        this.WriteLine("\n--- Start speech recognition using " + recoSource + " with " + this.getMode() + " mode in " + this.getDefaultLocale() + " language ----\n\n");
    }

    private void SendAudioHelper(String filename) {
        RecognitionTask doDataReco = new RecognitionTask(this.dataClient, this.getMode(), filename);
        try
        {
            doDataReco.execute().get(m_waitSeconds, TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            doDataReco.cancel(true);
            isReceivedResponse = FinalResponseStatus.Timeout;
        }
    }

    public void onFinalResponseReceived(final RecognitionResult response) {
        // we got the final result, so it we can end the mic reco.  No need to do this
        // for dataReco, since we already called endAudio() on it as soon as we were done
        // sending all the data.
        this.micClient.endMicAndRecognition();

        this.WriteLine("********* Final n-BEST Results *********");
        for (int i = 0; i < response.Results.length; i++) {
            this.WriteLine("[" + i + "]" + " Confidence=" + response.Results[i].Confidence +
                    " Text=\"" + response.Results[i].DisplayText + "\"");
        }

        this.WriteLine();

        if(!isTheEnd){
            //startMicrophone();
        }else{
            System.exit(1);
        }

    }

    /**
     * Called when a final response is received and its intent is parsed
     */
    public void onIntentReceived(final String payload) {
        this.WriteLine("--- Intent received by onIntentReceived() ---");
        this.WriteLine(payload);
        try {
            boolean beet = false;
            AssetManager am = getAssets();
            String[] folders = am.list("intents");
            MediaPlayer mp = new MediaPlayer();

            // set default intents (null)
            AssetFileDescriptor[] intentFiles = GetAssetsFromFolder("intents");

            // get assets from first folder
            if(folderMemory == 0){
                intentFiles = GetAssetsFromFolder(folders[folderMemory]);
                beet = true;
            }

            // get assets from current folder
            else{
                JSONObject jsonObj = new JSONObject(payload);
                JSONArray arr = jsonObj.getJSONArray("intents");
                String topScoringIntent = arr.getJSONObject(0).getString("intent");

                if (topScoringIntent.toLowerCase().trim().equals("vend")) {
                    beet = true;
                    intentFiles = GetAssetsFromFolder(folders[2]);
                }
                if (topScoringIntent.toLowerCase().trim().equals("monster")) {
                    beet = true;
                    intentFiles = GetAssetsFromFolder(folders[1]);
                }
            }

            // Check if trigger is caught
            if (fileMemory == 0 && folderMemory != 0) {
                JSONObject jsonObj = new JSONObject(payload);
                JSONArray arr = jsonObj.getJSONArray("intents");
                String topScoringIntent = arr.getJSONObject(0).getString("intent");

                if (topScoringIntent == folders[folderMemory].substring(1)) {
                    beet = true;
                    intentFiles = GetAssetsFromFolder(folders[folderMemory]);
                }
                    fileMemory = 0;
                    // next folder
            }

            // if filememory equals the number of files in current folder, check for trigger
            else if(fileMemory == GetAssetsFromFolder(folders[folderMemory]).length){
                JSONObject jsonObj = new JSONObject(payload);
                JSONArray arr = jsonObj.getJSONArray("intents");
                String topScoringIntent = arr.getJSONObject(0).getString("intent");

                // if topscore intent equals the next folder, go to next folder
                if (topScoringIntent.toLowerCase() == folders[folderMemory + 1].substring(1)) {
                    beet = true;
                }
                    if(folderMemory != 2){
                        // next folder
                        folderMemory++;
                    }
                    fileMemory = 0;
            }

            // if trigger is caught, play files
            if (intentFiles.length > 0 && beet) {
                    for (int intentFileCount = 0; intentFileCount < intentFiles.length; intentFileCount++) {

                    if(beet){
                        intentFileCount++;
                        beet = false;
                    }

                    int fileNumber = intentFileCount +1;
                    AssetFileDescriptor tf = getAssets().openFd("intents/" + folders[folderMemory] + "/" + fileNumber  + ".mp3");
                    playVerbalFeedback();

                    // play file
                    mp.setDataSource(tf.getFileDescriptor(), tf.getStartOffset(), tf.getLength());
                    mp.prepare();
                    mp.start();
                    synchronized(lock) {
                        lock.wait(5000 + mp.getDuration());
                    }
                    mp.reset();

                    fileMemory++;

                    if(intentFiles.length == intentFileCount+1){
                        fileMemory = 0;
                        folderMemory++;
                    }
                }
            } else {
                fileMemory = 0;
                }

            // If every file of a folder is played, start microphone for next intent
            if(fileMemory == GetAssetsFromFolder(folders[folderMemory]).length || fileMemory == 0){
                System.out.println("Start the microphone!");
                startMicrophone();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch(Exception ex){
            System.out.println(ex);
        }
        this.WriteLine();

    }


    public AssetFileDescriptor GetIntentFile(String intent)
    {
        try
        {
            AssetManager am = getAssets();
            String[] folders = am.list("intents");
            for(int folder = 0; folder < folders.length; folder++) {
                if (folderMemory > 0) {
                    folder = folderMemory;
                }

                AssetFileDescriptor[] files = new AssetFileDescriptor[getAssets().list("intents/" + folders[folder]).length];

                for (int file = 0; file < files.length; file++)
                {
                    if(fileMemory > 0){

                        file = fileMemory;
                    }
                    int fileNumber = file + 1;
                    return getAssets().openFd("intents/" + folders[folder] + "/" + fileNumber + ".mp3");
                }
            }
        }
        catch(Exception ex)
        {
            System.out.println(ex);
        }
        return null;
    }

    public AssetFileDescriptor[] GetAssetsFromFolder(String folderName){
        try{
            return new AssetFileDescriptor[getAssets().list("intents/" + folderName).length];
        }
        catch(Exception ex)
        {
            System.out.println(ex);
        }

        return null;
    }


    public void startMicrophone(){
//        if (null != this.micClient) {
//            System.out.println("mic was not null");
//            // we got the final result, so it we can end the mic reco.  No need to do this
//            // for dataReco, since we already called endAudio() on it as soon as we were done
//            // sending all the data.
//            this.micClient.endMicAndRecognition();
//        }
        this.micClient = null;
        if (this.micClient == null) {
            this.WriteLine("--- Start microphone dictation with Intent detection ----");

            this.micClient =
                    SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                            this,
                            this.getDefaultLocale(),
                            this,
                            this.getPrimaryKey(),
                            this.getLuisAppId(),
                            this.getLuisSubscriptionID());


            this.micClient.setAuthenticationUri(this.getAuthenticationUri());
        }
        this.micClient.startMicAndRecognition();
    }

    public void checkEndMicrophone(){
        if (null != this.micClient) {
            // we got the final result, so it we can end the mic reco.  No need to do this
            // for dataReco, since we already called endAudio() on it as soon as we were done
            // sending all the data.
            this.micClient.endMicAndRecognition();
        }
    }

    public void onPartialResponseReceived(final String response) {
        this.WriteLine("--- Partial result received by onPartialResponseReceived() ---");
        this.WriteLine(response);
        this.WriteLine();
    }

    public void onError(final int errorCode, final String response) {
        this._startButton.setEnabled(true);
        this.WriteLine("--- Error received by onError() ---");
        this.WriteLine("Error code: " + SpeechClientStatus.fromInt(errorCode) + " " + errorCode);
        this.WriteLine("Error text: " + response);
        this.WriteLine();
    }

    /**
     * Called when the microphone status has changed.
     * @param recording The current recording state
     */
    public void onAudioEvent(boolean recording) {
        this.WriteLine("--- Microphone status change received by onAudioEvent() ---");
        this.WriteLine("********* Microphone status: " + recording + " *********");
        if (recording) {
            this.WriteLine("Please start speaking.");
        }

        WriteLine();
        if (!recording) {
            this.micClient.endMicAndRecognition();
            this._startButton.setEnabled(true);
        }
    }

    /**
     * Writes the line.
     */
    private void WriteLine() {
        this.WriteLine("");
    }

    /**
     * Writes the line.
     * @param text The line to write.
     */
    private void WriteLine(String text) {
        this._logText.append(text + "\n");
    }

    /**
     * Handles the Click event of the RadioButton control.
     * @param rGroup The radio grouping.
     * @param checkedId The checkedId.
     */
    private void RadioButton_Click(RadioGroup rGroup, int checkedId) {
        // Reset everything
        if (this.micClient != null) {
            this.micClient.endMicAndRecognition();
            try {
                this.micClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            this.micClient = null;
        }

        if (this.dataClient != null) {
            try {
                this.dataClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            this.dataClient = null;
        }

        this.ShowMenu(false);
        this._startButton.setEnabled(true);
    }

    /**
     * Play verbal feedback audiofiles
     */

    private void playVerbalFeedback(){
        MediaPlayer mp2 = new MediaPlayer();
        Random rand = new Random();
        int randomFeedback = rand.nextInt(19 ) + 1;
        int randomActivation = rand.nextInt(2);

        try {
            if (mp2.isPlaying()){
                mp2.stop();
                mp2.release();
                mp2 = new MediaPlayer();
            }

            AssetFileDescriptor descriptor = getAssets().openFd("feedback/" + randomFeedback + ".mp3");
            mp2.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            if (skipFeedback == true){
                //Do nothing and set boolean to false to start else loop next time
                skipFeedback = false;
            }
            else if(skipFeedback == false && randomActivation == 1){
                mp2.prepare();
                mp2.start();
                synchronized(lock) { lock.wait(3500 + mp2.getDuration());}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Speech recognition with data (for example from a file or audio source).
     * The data is broken up into buffers and each buffer is sent to the Speech Recognition Service.
     * No modification is done to the buffers, so the user can apply their
     * own VAD (Voice Activation Detection) or Silence Detection
     *
     * @param dataClient
     * @param recoMode
     * @param filename
     */



    private class RecognitionTask extends AsyncTask<Void, Void, Void> {
        DataRecognitionClient dataClient;
        SpeechRecognitionMode recoMode;
        String filename;

        RecognitionTask(DataRecognitionClient dataClient, SpeechRecognitionMode recoMode, String filename) {
            this.dataClient = dataClient;
            this.recoMode = recoMode;
            this.filename = filename;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Note for wave files, we can just send data from the file right to the server.
                // In the case you are not an audio file in wave format, and instead you have just
                // raw data (for example audio coming over bluetooth), then before sending up any 
                // audio data, you must first send up an SpeechAudioFormat descriptor to describe 
                // the layout and format of your raw audio data via DataRecognitionClient's sendAudioFormat() method.
                // String filename = recoMode == SpeechRecognitionMode.ShortPhrase ? "whatstheweatherlike.wav" : "batman.wav";
                InputStream fileStream = getAssets().open(filename);
                int bytesRead = 0;
                byte[] buffer = new byte[1024];

                do {
                    // Get  Audio data to send into byte buffer.
                    bytesRead = fileStream.read(buffer);

                    if (bytesRead > -1) {
                        // Send of audio data to service. 
                        dataClient.sendAudio(buffer, bytesRead);
                    }
                } while (bytesRead > 0);

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            finally {
                dataClient.endAudio();
            }

            return null;
        }
    }
}
