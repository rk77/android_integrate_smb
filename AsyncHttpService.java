
package com.ruanke.smb.util;

import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import android.util.Log;

import jcifs.smb.SmbFile;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbRandomAccessFile;
import jcifs.smb.SmbFileInputStream;

/**
 *
 * @author brlalexu
 */
public class AsyncHttpService {
    private static final String TAG = AsyncHttpService.class.getSimpleName();

    private static final int PORT = 8707;

    public static enum Status {
        OK(200, "OK"),
        CREATED(201, "Created"),
        ACCEPTED(202, "Accepted"),
        NO_CONTENT(204, "No Content"),
        PARTIAL_CONTENT(206, "Partial Content"),

        BAD_REQUEST(400, "Bad Request"),
        UNAUTHORIZED(401, "Unauthorized"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),
        METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
        NOT_ACCEPTABLE(406, "Not Acceptable"),
        REQUEST_TIMEOUT(408, "Request Timeout"),
        CONFLICT(409, "Conflict"),
        GONE(410, "Gone"),
        LENGTH_REQUIRED(411, "Length Required"),
        PRECONDITION_FAILED(412, "Precondition Failed"),
        PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
        UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
        RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
        EXPECTATION_FAILED(417, "Expectation Failed"),
        TOO_MANY_REQUESTS(429, "Too Many Requests"),

        INTERNAL_ERROR(500, "Internal Server Error"),
        NOT_IMPLEMENTED(501, "Not Implemented"),
        SERVICE_UNAVAILABLE(503, "Service Unavailable"),
        UNSUPPORTED_HTTP_VERSION(505, "HTTP Version Not Supported");

        private final int requestStatus;
        private final String description;

        Status(int requestStatus, String description) {
            this.requestStatus = requestStatus;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public int getRequestStatus() {
            return requestStatus;
        }
    }

    private NtlmPasswordAuthentication mAuth;
    private String mPath = null;
    private SmbFile mFile = null;
    private SmbFileInputStream mSmbFileIs = null;

    private static class SingletonHolder {
        public static AsyncHttpService instance = new AsyncHttpService();
    }

    /**
     * Get an instance of the AsyncHttpService class.
     * @return the singleton instance of AsyncHttpService
     */
    public static AsyncHttpService getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * Private constructor to enforce singleton pattern.
     */
    private AsyncHttpService() {
        AsyncHttpServer server = new AsyncHttpServer();
        RequestCallback rc = new RequestCallback();
        server.addAction("OPTIONS", "[\\d\\D]*", rc);
        server.get("[\\d\\D]*", rc);
        server.post("[\\d\\D]*", rc);
        server.listen(PORT);
        Log.i(TAG, "Http server running on http://localhost:8707/");
    }


    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public void setAuth(NtlmPasswordAuthentication auth) {
        mAuth = auth;
    }

    private class RequestCallback implements HttpServerRequestCallback {
        public RequestCallback() {};
        @Override
        public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

            String uri = request.getPath();
            try {
                uri = URLDecoder.decode(uri, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            Multimap headers = request.getHeaders().getMultiMap();
            Log.i(TAG, "onRequest, uri: " + uri);

            String path = "smb://" + uri.substring(5);
            if (!path.equals(mPath)) {
                mPath = path;
                try {
                    mFile = new SmbFile(mPath, mAuth);
                } catch(Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            String range = headers.getString("range");
            if (range == null) {
                return;
            }
            range = range.substring(6);
            Log.i(TAG, "serve, range: " + range);
            int index = range.indexOf('-');
            int start = 0;
            int end = -1;
            Log.i(TAG, "index: " + index);
            if (index > 0) {
                String min = range.substring(0, index);
                start = Integer.parseInt(min);
                String max = range.substring(index + 1);
                Log.i(TAG, "max: " + max);
                if (max != null) {
                    Log.i(TAG, "max1: " + max);
                    //end = Long.parseLong(max);
                }
            } else {
                return;
            }

            Log.i(TAG, "serve, start:" + start + ", end: " + end + ", path: " + path);
            try {
                long fileLen = mFile.length();
                if (start >= (fileLen - 1)) {
                    Log.i(TAG, "return respond 0");
                    return;
                }
                if (start == 0) {
                    Log.i(TAG, "return respond 1");
                    response.sendStream(mFile.getInputStream(), fileLen);
                    return;
                }
                if (end <= start) {
                    end = start + 1024 * 1024 * 3 - 1;
                }
                if (end >= fileLen) {
                    end = (int)fileLen - 1;
                }
                //SmbFileInputStream is = new SmbFileInputStream(mFile);
                BufferedInputStream bis = new BufferedInputStream(mFile.getInputStream());
                byte[] data = new byte[end - start + 1];
                bis.skip(start);
                int size = bis.read(data, 0, data.length);
                //Log.i(TAG, "data: " + bytesToHexString(data));
                //InputStream input = new ByteArrayInputStream(data);
                Log.i(TAG, "return respond size: " + size + ",end: " + end);
                bis.close();
                //respond.addHeader("Accept-Ranges", "bytes");
                //respond.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLen);
                response.getHeaders().set("Accept-Ranges", "bytes");
                response.getHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + fileLen);
                response.code(206).send("application/octet-stream", data);
                //response.sendStream(bis, fileLen - start);
            } catch (Exception e) {
                 Log.e(TAG, "error: " + e.getMessage());
                 e.printStackTrace();
            } finally {
            }
        }
    }
}
