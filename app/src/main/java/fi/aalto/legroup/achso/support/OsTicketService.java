package fi.aalto.legroup.achso.support;

import java.util.Map;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;

/**
 * Created by lassi on 14.11.14.
 */
public interface OsTicketService {

    @POST("/api/tickets.json")
    @Headers("User-Agent: Ach so! (Android)")
    public void sendFeedback(@Header("X-API-Key") String apiKey, @Body Map<String, String> body,
                             Callback<String> callback);

}
