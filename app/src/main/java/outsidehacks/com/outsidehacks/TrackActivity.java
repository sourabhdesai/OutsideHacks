package outsidehacks.com.outsidehacks;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.client.Response;


public class TrackActivity extends ActionBarActivity {

    private static String ACCESS_TOKEN;
    private static String USER_ID;
    private static String PLAYLIST_ID;
    private String artistQuery;
    private String trackQuery;

    private EditText artistET;
    private EditText trackET;
    private ImageView trackAlbumIV;
    private Button saveTrackToSpotifyBtn;
    private TextView artistTV;
    private TextView trackTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);
        artistET = (EditText)findViewById(R.id.et_artist);
        trackET = (EditText)findViewById(R.id.et_track);
        saveTrackToSpotifyBtn = (Button)findViewById(R.id.btn_save_track);
        trackAlbumIV = (ImageView)findViewById(R.id.iv_track);
        trackTV = (TextView)findViewById(R.id.tv_title);
        artistTV = (TextView)findViewById(R.id.tv_artist);

        ACCESS_TOKEN = getIntent().getStringExtra("ACCESS_TOKEN");
        USER_ID = getIntent().getStringExtra("USER_ID");
        PLAYLIST_ID = getIntent().getStringExtra("PLAYLIST_ID");

        SpotifyApi api = new SpotifyApi();
        api.setAccessToken(ACCESS_TOKEN);
        final SpotifyService spotify = api.getService();

        saveTrackToSpotifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                artistQuery = artistET.getText().toString();
                trackQuery = trackET.getText().toString();
                String query = trackQuery.replace(" ", "+") + '+' + artistQuery.replace(" ", "+");
                Log.v("TrackActivity", query);
//                Map<String, Object> options = new HashMap<String, Object>();
//                options.put()
//                spotify.searchTracks(query, options, new SpotifyCallback<TracksPager>() {
//                    @Override
//                    public void failure(SpotifyError spotifyError) {
//
//                    }
//
//                    @Override
//                    public void success(TracksPager tracksPager, Response response) {
//
//                    }
//                });
                spotify.searchTracks(trackQuery, new SpotifyCallback<TracksPager>() {
                    @Override
                    public void failure(SpotifyError spotifyError) {
                        Log.v("TrackActivity", "Error: " + spotifyError);
                    }

                    @Override
                    public void success(TracksPager tracksPager, Response response) {
                        List<Track> tracks = tracksPager.tracks.items;
                        for (Track track: tracks) {
//                            Log.v("TrackActivity", track.name);
                            List<ArtistSimple> artists = track.artists;
                            for (ArtistSimple artist: artists) {
                                if(artist.name.toLowerCase().replace(" ", "").equals(artistQuery.toLowerCase().replace(" ", ""))){
                                    Log.v("TrackActivity", "Track Selected: " + track.name);
                                    trackTV.setText(track.name);
                                    artistTV.setText(artist.name);
                                    Picasso.with(getApplicationContext()).load(track.album.images.get(0).url).into(trackAlbumIV);
                                    Map<String, Object> parameters = new HashMap<String, Object>();
                                    parameters.put("uris", track.uri);
                                    spotify.addTracksToPlaylist(USER_ID, PLAYLIST_ID, parameters, parameters, new SpotifyCallback<Pager<PlaylistTrack>>() {
                                        @Override
                                        public void failure(SpotifyError spotifyError) {
                                            Log.v("TrackActivity", "Error: " + spotifyError);
                                        }

                                        @Override
                                        public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                                            Log.v("TrackActivity", "Successfully added to playlist");
                                        }
                                    });
                                    return;
                                } else {
                                    Log.v("TrackActivity",artist.name.toLowerCase().replace(" ", "") + " to " + artistQuery.toLowerCase().replace(" ", ""));
                                    Log.v("TrackActivity", artist.name + " : " + track.name);
                                }
                            }
                        }
                    }
                });

            }
        });



    }

}
