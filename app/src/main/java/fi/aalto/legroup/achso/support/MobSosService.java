package fi.aalto.legroup.achso.support;

import java.util.Map;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.Path;

public interface MobSosService {

    @POST("/surveys/{id}/responses")
    public void postResponses(@Path("id") int surveyId, @Body Map<String, String> responses,
                              Callback<Response> callback);

}
