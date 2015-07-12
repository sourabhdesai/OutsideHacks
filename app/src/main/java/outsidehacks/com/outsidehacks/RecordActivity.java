package outsidehacks.com.outsidehacks;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import outsidehacks.com.outsidehacks.gracenote.ExtAudioRecorder;
import outsidehacks.com.outsidehacks.gracenote.GracenoteApiKeys;
import outsidehacks.com.outsidehacks.gracenote.GracenoteApiService;
import outsidehacks.com.outsidehacks.gracenote.GracenoteLiveIDService;
import outsidehacks.com.outsidehacks.gracenote.GracenoteRequestBodyTemplates;
import outsidehacks.com.outsidehacks.gracenote.ResponseUtils;
import outsidehacks.com.outsidehacks.gracenote.singletons.GracenoteApiServiceProvider;
import outsidehacks.com.outsidehacks.gracenote.singletons.GracenoteLiveIDServiceProvider;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedString;


public class RecordActivity extends ActionBarActivity {

    private static final String USER_ID_KEY = "gn_user_id";
    private static final String SHARED_PREFS_FILENAME = "spottrack_shared_prefs";
    private static final String AUDIO_RECORDER_FOLDER = "recordings";
    private static final String RECORDING_FILENAME = "recording.wav";

    private static final int RECORDING_LENGTH_SECONDS = 9;

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private String userID = null;

    Button recordButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        this.recordButton = (Button) this.findViewById(R.id.record_button);

        final SharedPreferences sharedPrefs = getSharedPreferences(SHARED_PREFS_FILENAME, MODE_PRIVATE);
        String userID = sharedPrefs.getString(USER_ID_KEY, null);
        if (userID == null) {
            this.getUserID(new Callback<JSONObject>() {
                @Override
                public void success(JSONObject resObj, Response response) {
                    try {
                        RecordActivity.this.userID = resObj
                            .getJSONObject("RESPONSE")
                            .getJSONArray("USER")
                            .getJSONObject(0)
                            .getString("VALUE");

                    } catch (JSONException e) {
                        e.printStackTrace();

                        // lol
                        RecordActivity.this.userID = GracenoteApiKeys.FALLBACK_USER_ID;
                    } finally {
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        editor.putString(USER_ID_KEY, RecordActivity.this.userID);

                        boolean success = editor.commit();

                        if (!success) {
                            System.err.println("Unsuccessful SharedPreferences.Editor commit()...:(");
                            showErrorDialog();
                        } else {
                            setRecordButtonActive();
                        }
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    if (error != null) {
                        error.printStackTrace();
                    }
                    showErrorDialog();
                }
            });
        } else {
            this.setRecordButtonActive();
        }
    }

    private void showErrorDialog() {
        Toast.makeText(RecordActivity.this, "Failed to load", Toast.LENGTH_LONG).show();
    }

    private String getRecordingFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + RECORDING_FILENAME);
    }

    private void getUserID(final Callback<JSONObject> cb) {
        GracenoteApiService apiService = GracenoteApiServiceProvider.getInstance();

        String reqBody = GracenoteRequestBodyTemplates.getRegisterUserReqBody();
        apiService.registerApi(reqBody, new Callback<Response>() {
            @Override
            public void success(Response response, Response _) {
                try {
                    JSONObject resObj = ResponseUtils.getResponseBodyJSON(response);
                    cb.success(resObj, response);
                } catch (IOException e) {
                    e.printStackTrace();
                    cb.failure(null);
                } catch (JSONException e) {
                    e.printStackTrace();
                    cb.failure(null);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                cb.failure(error);
            }
        });
    }


    private void setRecordButtonActive() {
        this.recordButton.setEnabled(true);
        this.recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Record Button Pressed");
                RecordActivity.this.startRecording();
            }
        });
    }

    private String getSelectedArtist() {
        return "TV On The Radio";
    }

    private void startRecording() {
        System.out.println("startRecording()");
        final File recordingFile = new File(getRecordingFilename());

        try {
            recordingFile.createNewFile(); // overwrites previous file
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog();
            return;
        }

        final Date startTime = new Date();

        final ExtAudioRecorder extAudioRecorder = ExtAudioRecorder.getInstanse(false);

        extAudioRecorder.setOutputFile(recordingFile.getPath());
        extAudioRecorder.prepare();
        extAudioRecorder.start();

        new CountDownTimer(RECORDING_LENGTH_SECONDS * 1000, RECORDING_LENGTH_SECONDS * 1000 / 2) {
            @Override
            public void onTick(long millisUntilFinished) {
                System.out.println("onTick: millisUntilFinished=" + millisUntilFinished);
            }

            @Override
            public void onFinish() {
                System.out.println("Done Recording");
                // Stop recording and upload file
                extAudioRecorder.stop();
                extAudioRecorder.release();

                uploadRecordingFile(recordingFile, getSelectedArtist(), new Callback<JSONObject>() {
                    @Override
                    public void success(JSONObject result, Response response) {
                        try {
                            String artist = result.getString("artist");
                            String song = result.getString("song");

                            String msg = "Artist: " + artist + ", Song: " + song;
                            System.out.println(msg);

                            Toast.makeText(RecordActivity.this, msg, Toast.LENGTH_LONG).show();

                            //startTrackActivity(artist, song);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showErrorDialog();
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        error.printStackTrace();
                        byte[] bodyBuff = new byte[(int) error.getResponse().getBody().length()];
                        try {
                            error.getResponse().getBody().in().read(bodyBuff);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.err.print("RetrofitError: \n" + new String(bodyBuff));
                        showErrorDialog();
                    }
                });
            }
        }.start();
    }

    public void startTrackActivity(String artist, String song) {
        // Start TrackActivity
        Intent trackIntent = new Intent(RecordActivity.this, TrackActivity.class);
        trackIntent.putExtra("artist", artist);
        trackIntent.putExtra("song", song);
        startActivity(trackIntent);
    }

    private void uploadRecordingFile(File audioFile, final String artist, final Callback<JSONObject> cb) {
        GracenoteLiveIDService liveIDService = GracenoteLiveIDServiceProvider.getInstance();

        TypedFile typedAudioFile = new TypedFile("audio/wav", audioFile);
        TypedString typedArtist = new TypedString(artist);

        liveIDService.IdentifySongInFile(typedAudioFile, typedArtist, new Callback<Response>() {
            @Override
            public void success(Response response, Response _) {
                try {
                    JSONObject resObj = ResponseUtils.getResponseBodyJSON(response);

                    JSONArray matches = resObj.getJSONArray("matches");
                    JSONObject match = matches.getJSONObject(0); // Ordered by descending certainty
                    String songName = match.getString("song_name");

                    JSONObject result = new JSONObject().put("song", songName).put("artist", artist);

                    cb.success(result, response);
                } catch (IOException e) {
                    e.printStackTrace();
                    cb.failure(null);
                } catch (JSONException e) {
                    e.printStackTrace();
                    cb.failure(null);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                cb.failure(error);
            }
        });
    }

}
