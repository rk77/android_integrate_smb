
package com.ranke.smb.util;

import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import android.util.Log;

import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.http.IHTTPSession;

import jcifs.smb.SmbFile;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbRandomAccessFile;
import jcifs.smb.SmbFileInputStream;

/**
 *
 * @author brlalexu
 */
public class HttpServer extends NanoHTTPD {
    private static final String TAG = HttpServer.class.getSimpleName();

    private static final int PORT = 8707;

    private NtlmPasswordAuthentication mAuth;

    private static class SingletonHolder {
        public static HttpServer instance = new HttpServer();
    }

    /**
     * Get an instance of the HttpServer class.
     * @return the singleton instance of HttpServer
     */
    public static HttpServer getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * Private constructor to enforce singleton pattern.
     */
    private HttpServer() {
        super(PORT);
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e) {
            Log.e(TAG, "start server error");
        }
        Log.i(TAG, "Http server running on http://localhost:8707/");
    }

    private String mPath = null;
    private SmbFile mFile = null;
    private SmbFileInputStream mSmbFileIs = null;
    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        Log.i(TAG, "serve, params: " + headers);
        Method method = session.getMethod();
        String uri = session.getUri();
        Log.i(TAG, "serve, method: " + method + ", uri: " + uri);

        String path = "smb://" + uri.substring(5);
        if (!path.equals(mPath)) {
            mPath = path;
            try {
                mFile = new SmbFile(mPath, mAuth);
                mSmbFileIs = new SmbFileInputStream(mFile);

            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        if (headers == null || !headers.containsKey("range")) {
            return null;
        }
        String range = headers.get("range").substring(6);
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
            return null;
        }
        Log.i(TAG, "serve, start:" + start + ", end: " + end);

        Log.i(TAG, "serve, start:" + start + ", end: " + end + ", path: " + path);
        try {
            long fileLen = mFile.length();
            if (start >= (fileLen - 1)) {
                Log.i(TAG, "return respond 0");
                return Response.newChunkedResponse(Status.RANGE_NOT_SATISFIABLE, "video/mpeg", null);
            }
            if (start == 0) {
                Log.i(TAG, "return respond 1");
                return Response.newFixedLengthResponse(Status.PARTIAL_CONTENT, "video/mpeg", mFile.getInputStream(), fileLen);
            }
            if (end <= start) {
                end = start + 1024 * 512 - 1;
            }
            if (end >= fileLen) {
                end = (int)fileLen - 1;
            }
            //SmbRandomAccessFile is = new SmbRandomAccessFile(mFile, "r");
            //SmbFileInputStream is = new SmbFileInputStream(mFile);
            byte[] data = new byte[end - start + 1];
            //sraf.read(data, start, 512);
            InputStream is = mFile.getInputStream();
            is.skip(start);
            int size = is.read(data, 0, data.length);
            //Log.i(TAG, "data: " + bytesToHexString(data));
            //InputStream input = new ByteArrayInputStream(data);
            Log.i(TAG, "return respond size: " + size);
            Response respond = Response.newFixedLengthResponse(Status.PARTIAL_CONTENT, "video/mpeg", data);
            respond.addHeader("Accept-Ranges", "bytes");
            respond.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLen);
            //is.close();
            return respond;
           // return Response.newFixedLengthResponse(Status.OK, "video/mpeg", mFile.getInputStream(), mFile.length());
        } catch (Exception e) {
            Log.e(TAG, "error: " + e.getMessage());
            e.printStackTrace();
        } finally {
        }
        return null;
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
}
