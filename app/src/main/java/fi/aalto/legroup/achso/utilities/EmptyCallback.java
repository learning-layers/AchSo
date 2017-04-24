package fi.aalto.legroup.achso.utilities;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by mat on 09/04/2017.
 */

public class EmptyCallback implements Callback {
    @Override
    public void onFailure(Request request, IOException e) {
        System.out.println(request);
    }

    @Override
    public void onResponse(Response response) throws IOException {
        System.out.println(response);
    }
}
