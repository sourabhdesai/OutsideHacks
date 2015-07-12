package outsidehacks.com.outsidehacks;

import android.content.Intent;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginActivity extends Activity implements PlayerNotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "555de185a8e241bd95f34f7dd43f3d4c";
    private static final String REDIRECT_URI = "outsidehacks://callback";

    private Button spotifyLoginBtn;

    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 1337;
    private static final String PLAYLIST_DEFAULT = "OutsideHacks";
    private static String ACCESS_TOKEN;
    private static String USER_ID;
    private static String PLAYLIST_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        AuthenticationRequest.Builder builder
                = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "playlist-modify-public", "playlist-modify-private", "playlist-read-private"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Log.d("LoginActivity", response.getAccessToken());
                ACCESS_TOKEN = response.getAccessToken();
                fetchOrCreateDefaultPlaylist(response.getAccessToken());
            }
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("LoginActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("LoginActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("LoginActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("LoginActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("LoginActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("LoginActivity", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("LoginActivity", "Playback error received: " + errorType.name());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void fetchOrCreateDefaultPlaylist(String accessToken) {
        SpotifyApi api = new SpotifyApi();
        api.setAccessToken(accessToken);
        final SpotifyService spotify = api.getService();
        spotify.getMe(new SpotifyCallback<UserPrivate>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                Log.v("LoginActivity", "Error: " + spotifyError);
            }

            @Override
            public void success(final UserPrivate userPrivate, Response response) {
                Log.v("LoginActivity","Retrieved User Id: " + userPrivate.id);
                USER_ID = userPrivate.id;
                spotify.getPlaylists(userPrivate.id, new SpotifyCallback<Pager<PlaylistSimple>>() {
                    @Override
                    public void failure(SpotifyError spotifyError) {
                        Log.v("LoginActivity", "Error: " + spotifyError);
                    }

                    @Override
                    public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                        List<PlaylistSimple> playlists = playlistSimplePager.items;
                        for (PlaylistSimple playlist:playlists) {
                            Log.v("LoginActivity", "Retrieved Playlist: "+ playlist.name);
                            if (playlist.name.equals(PLAYLIST_DEFAULT)) {
                                Log.v("LoginActivity", "Default Playlist EXISTS");
                                PLAYLIST_ID = playlist.id;
                                moveToTrackActivity();
                                return;
                            }
                        }
                        Log.v("LoginActivity", "Default Playlist DOES NOT EXIST");
                        Map<String, Object> body = new HashMap<String, Object>();
                        body.put("name", PLAYLIST_DEFAULT);
                        body.put("public", false);
                        spotify.createPlaylist(userPrivate.id, body, new SpotifyCallback<Playlist>() {
                            @Override
                            public void failure(SpotifyError spotifyError) {
                                Log.v("LoginActivity", "Error: " + spotifyError);
                            }

                            @Override
                            public void success(Playlist playlist, Response response) {
                                Log.v("LoginActivity", "Created Default Playlist");
                                PLAYLIST_ID = playlist.id;
                                moveToTrackActivity();
                                return;
                            }
                        });
                    }
                });
            }
        });

    }

    public void moveToTrackActivity() {
        Intent intent = new Intent(this, TrackActivity.class);
        intent.putExtra("ACCESS_TOKEN", ACCESS_TOKEN);
        intent.putExtra("USER_ID", USER_ID);
        intent.putExtra("PLAYLIST_ID", PLAYLIST_ID);
        startActivity(intent);
        finish();
    }
}