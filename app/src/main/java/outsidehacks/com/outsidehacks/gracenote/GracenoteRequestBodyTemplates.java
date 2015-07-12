package outsidehacks.com.outsidehacks.gracenote;

/**
 * Created by sourabhdesai on 7/11/15.
 */
public class GracenoteRequestBodyTemplates {

    public static final String TRACK_INFO =
            "<QUERIES>\n" +
            "  <LANG>eng</LANG>\n" +
            "  <AUTH>\n" +
            "    <CLIENT>" + GracenoteApiKeys.CLIENT_ID + "</CLIENT>\n" +
            "    <USER>{{userId}}</USER>\n" +
            "  </AUTH>\n" +
            "  <QUERY CMD=\"ALBUM_SEARCH\">\n" +
            "    <TEXT TYPE=\"ARTIST\">{{artist}}</TEXT>\n" +
            "    <TEXT TYPE=\"TRACK_TITLE\">{{song}}</TEXT>\n" +
            "  </QUERY>\n" +
            "</QUERIES>\n";

    public static final String REGISTER_USER =
            "<QUERIES>\n" +
            "\t<QUERY CMD=\"REGISTER\">\n" +
            "\t\t<CLIENT>" + GracenoteApiKeys.CLIENT_ID + "</CLIENT>\n" +
            "\t</QUERY>\n" +
            "</QUERIES>\n";


    public static String getTrackInfoReqBody(String artist, String song, String userId) {
        return TRACK_INFO
                .replace("{{artist}}", artist)
                .replace("{{song}}", song)
                .replace("{{userId}}", userId);
    }

    public static String getRegisterUserReqBody() {
        return REGISTER_USER;
    }

}
