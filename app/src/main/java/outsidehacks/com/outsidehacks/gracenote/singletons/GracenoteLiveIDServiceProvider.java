package outsidehacks.com.outsidehacks.gracenote.singletons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import outsidehacks.com.outsidehacks.gracenote.GracenoteApiService;
import outsidehacks.com.outsidehacks.gracenote.GracenoteLiveIDService;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Created by sourabhdesai on 7/11/15.
 */
public class GracenoteLiveIDServiceProvider {

    private static GracenoteLiveIDService instance;
    private static final String BASE_URL = "http://live-id-hack.elasticbeanstalk.com/";

    public static GracenoteLiveIDService getInstance() {
        if (instance == null) {
            Gson gson = new GsonBuilder()
                    .create();

            // Create rest adapter from RetroFit. Initialize endpoint
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(BASE_URL)
                    .setConverter(new GsonConverter(gson))
                    .build();

            instance = restAdapter.create(GracenoteLiveIDService.class);
        }

        return instance;
    }

}
