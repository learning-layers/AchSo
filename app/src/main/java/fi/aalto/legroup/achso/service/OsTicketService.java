package fi.aalto.legroup.achso.service;

import com.google.gson.Gson;
import com.squareup.okhttp.Response;

import org.json.JSONObject;


import java.util.HashMap;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.FormUrlEncoded;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
/**
 * Created by lassi on 14.11.14.
 */
public interface OsTicketService {
    @Headers("User-Agent: AchSo! Android")
    @POST("/api/tickets.json")
    void sendFeedback(@Header("X-API-Key") String apiKey, @Body HashMap<String, String> map, Callback<String> callback);
}
