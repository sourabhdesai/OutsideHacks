package outsidehacks.com.outsidehacks;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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


public class RecordActivity extends ActionBarActivity implements AdapterView.OnItemSelectedListener {

    private static final String USER_ID_KEY = "gn_user_id";
    private static final String SHARED_PREFS_FILENAME = "spottrack_shared_prefs";
    private static final String AUDIO_RECORDER_FOLDER = "recordings";
    private static final String RECORDING_FILENAME = "recording.wav";

    private static final int RECORDING_LENGTH_SECONDS = 9;

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private String SPOTIFY_ACCESS_TOKEN;
    private String SPOTIFY_USER_ID;
    private String SPOTIFY_PLAYLIST_ID;

    private String userID = null;

    ImageButton recordButton = null;
    TextView countdownTextView = null;
    Spinner artistSpinner = null;

    List<String> artistList = null;
    String selectedArtist = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        SPOTIFY_ACCESS_TOKEN = getIntent().getStringExtra("ACCESS_TOKEN");
        SPOTIFY_USER_ID = getIntent().getStringExtra("USER_ID");
        SPOTIFY_PLAYLIST_ID = getIntent().getStringExtra("PLAYLIST_ID");

        this.recordButton = (ImageButton)this.findViewById(R.id.record_button);
        this.countdownTextView = (TextView) this.findViewById(R.id.countdown_textview);
        this.artistSpinner = (Spinner) this.findViewById(R.id.spin_artist);

        this.loadAvailableArtists();

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

    private void loadAvailableArtists() {
        GracenoteLiveIDService liveIDService = GracenoteLiveIDServiceProvider.getInstance();
        liveIDService.GetLiveIDArtists(new Callback<Response>() {
            @Override
            public void success(Response response, Response _) {
                try {
                    JSONObject resObj = ResponseUtils.getResponseBodyJSON(response);
                    JSONArray artistJSONArray = resObj.getJSONArray("artists");

                    RecordActivity.this.artistList = new ArrayList<String>(artistJSONArray.length());

                    for (int i = 0; i < artistJSONArray.length(); i++) {
                        String artist = artistJSONArray.getString(i);
                        artistList.add(artist);
                    }

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(RecordActivity.this, android.R.layout.simple_spinner_item, artistList);
                    artistSpinner.setAdapter(arrayAdapter);
                    artistSpinner.setOnItemSelectedListener(RecordActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                    showErrorDialog();
                } catch (JSONException e) {
                    e.printStackTrace();
                    showErrorDialog();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                error.printStackTrace();
                showErrorDialog();
            }
        });
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
                recordButton.setImageDrawable(getResources().getDrawable(R.mipmap.mic_recording_tan));
                Log.v("RecordActivity", "Record Button Pressed");
                RecordActivity.this.startRecording();
            }
        });
    }

    private String getSelectedArtist() {
        return this.selectedArtist;
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

        new CountDownTimer(RECORDING_LENGTH_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsTillFinish = millisUntilFinished / 1000;
                RecordActivity.this.countdownTextView.setText(String.valueOf(secondsTillFinish));

            }

            @Override
            public void onFinish() {
                System.out.println("Done Recording");
                RecordActivity.this.countdownTextView.setText("");
                // Stop recording and upload file
                extAudioRecorder.stop();
                extAudioRecorder.release();
                recordButton.setImageDrawable(getResources().getDrawable(R.mipmap.mic_default_tan));

                String artist = getSelectedArtist();

                if (artist == null) {
                    Toast.makeText(RecordActivity.this, "Please Select an Artist", Toast.LENGTH_LONG).show();
                } else {
                    final ProgressDialog progress = new ProgressDialog(RecordActivity.this);
                    progress.setTitle("Analyzing");
                    progress.setMessage("Hittin up " + artist + "...");
                    progress.show();

                    uploadRecordingFile(recordingFile, artist, new Callback<JSONObject>() {
                        @Override
                        public void success(JSONObject result, Response response) {

                            // To dismiss the dialog
                            progress.dismiss();

                            try {
                                String artist = result.getString("artist");
                                String song = result.getString("song");

                                String msg = "Artist: " + artist + ", Song: " + song;
                                System.out.println(msg);

                                startTrackActivity(artist, song);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                showErrorDialog();
                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {

                            // To dismiss the dialog
                            progress.dismiss();

                            Toast.makeText(RecordActivity.this, "Couldn't find a match", Toast.LENGTH_LONG).show();

                            error.printStackTrace();
                            byte[] bodyBuff = new byte[(int) error.getResponse().getBody().length()];
                            try {
                                error.getResponse().getBody().in().read(bodyBuff);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.err.print("RetrofitError: \n" + new String(bodyBuff));
                        }
                    });
                }

            }
        }.start();
    }

    public void startTrackActivity(String artist, String song) {
        // Start TrackActivity
        Intent trackIntent = new Intent(RecordActivity.this, TrackActivity.class);
        trackIntent.putExtra("artist", artist);
        trackIntent.putExtra("song", song);
        trackIntent.putExtra("ACCESS_TOKEN", SPOTIFY_ACCESS_TOKEN);
        trackIntent.putExtra("USER_ID", SPOTIFY_USER_ID);
        trackIntent.putExtra("PLAYLIST_ID", SPOTIFY_PLAYLIST_ID);
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.selectedArtist = this.artistList.get(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
