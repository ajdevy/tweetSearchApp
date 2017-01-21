package com.twittersearch.app.data.json;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import java.util.HashMap;
import java.util.Map;

public class JacksonRequest<T> extends JsonRequest<T> {

    private final Map<String, String> headers;
    private Class<T> responseType;

    /**
     * Creates a new request.
     *
     * @param method        the HTTP method to use
     * @param url           URL to fetch the JSON from
     * @param headers       Headers to pass to the HTTP request
     * @param requestData   A {@link Object} to post and convert into json as the request. Null is allowed and indicates no parameters will be posted along with request.
     * @param listener      Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public JacksonRequest(int method, String url, Map<String, String> headers, Object requestData, Class<T> responseType, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(method, url, (requestData == null) ? null : Mapper.string(requestData), listener, errorListener);
        this.responseType = responseType;
        this.headers = headers;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(Mapper.objectOrThrow(jsonString, responseType), HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        if (headers != null) {
            return headers;
        } else {
            return new HashMap<>();
        }
    }
}