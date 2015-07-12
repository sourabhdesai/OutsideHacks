package outsidehacks.com.outsidehacks.gracenote;

import java.util.Map;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.PartMap;
import retrofit.mime.FormUrlEncodedTypedOutput;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedString;

/**
 * Created by sourabhdesai on 7/11/15.
 */
public interface GracenoteLiveIDService {

    @GET("/api/v2/liveid/artists")
    void GetLiveIDArtists(Callback<Response> cb);

    @Multipart
    @POST("/api/v2/liveid/match/")
    void IdentifySongInFile(@Part("audio_file") TypedFile audioFile, @Part("artist_name") TypedString artistName, Callback<Response> cb);



}
