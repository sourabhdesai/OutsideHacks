package outsidehacks.com.outsidehacks.gracenote;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.POST;

/**
 * Created by sourabhdesai on 7/11/15.
 */
public interface GracenoteApiService {

    @Headers({
            "Content-Type: application/xml"
    })
    @POST("/webapi/json/1.0/")
    void registerApi(@Body String registrationBody, Callback<Response> cb);

    @Headers({
            "Content-Type: application/xml"
    })
    @POST("/webapi/json/1.0/")
    void getTrackInfo(@Body String queryBody, Callback<Response> cb);

}
