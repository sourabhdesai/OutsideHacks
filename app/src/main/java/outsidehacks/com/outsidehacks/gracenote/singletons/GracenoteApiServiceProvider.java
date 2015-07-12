package outsidehacks.com.outsidehacks.gracenote.singletons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import outsidehacks.com.outsidehacks.gracenote.GracenoteApiKeys;
import outsidehacks.com.outsidehacks.gracenote.GracenoteApiService;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Created by sourabhdesai on 7/11/15.
 */
public class GracenoteApiServiceProvider {

    private static GracenoteApiService instance;

    public static GracenoteApiService getInstance() {
        if (instance == null) {
            int hyphenIdx = GracenoteApiKeys.CLIENT_ID.indexOf('-');
            String baseUrlPrefix = GracenoteApiKeys.CLIENT_ID.substring(0, hyphenIdx);

            String baseUrl = "https://c{{prefix}}.web.cddbp.net"
                                .replace("{{prefix}}", baseUrlPrefix);

            Gson gson = new GsonBuilder()
                    .create();

            // Create rest adapter from RetroFit. Initialize endpoint
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(baseUrl)
                    .setConverter(new GsonConverter(gson))
                    .build();

            instance = restAdapter.create(GracenoteApiService.class);
        }

        return instance;
    }

}
