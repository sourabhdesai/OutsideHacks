package outsidehacks.com.outsidehacks.gracenote;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;

import retrofit.client.Response;

/**
 * Created by sourabhdesai on 7/11/15.
 */
public class ResponseUtils {

    public static String getResponseBody(Response res) throws IOException {
        long bodyLen = res.getBody().length();
        byte[] bodyBuff = new byte[(int) bodyLen];
        res.getBody().in().read(bodyBuff);
        return new String(bodyBuff);
    }

    public static JSONObject getResponseBodyJSON(Response res) throws IOException, JSONException {
        String body = getResponseBody(res);
        JSONObject obj = new JSONObject(body);

        return obj;
    }

}
