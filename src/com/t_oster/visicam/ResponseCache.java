/*
 * Copyright (c) 2015 Max Gaukler <development@maxgaukler.de>
 */
package com.t_oster.visicam;

import gr.ktogias.NanoHTTPD.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper for NanoHTTPD.Response which offers a 'factory' to create copies
 * (NanoHTTPD.Response itself has no copy constructor)
 */
public class ResponseCache {

    private final byte[] data;
    private Properties header;
    private String mimeType;
    private String status;

    /**
     * initialize cache from a Response object. The given object must not be
     * modified or used anymore.
     *
     * @param r
     */
    public ResponseCache(Response r) {
        mimeType = r.mimeType;
        status = r.status;
        header = r.header;

            // convert InputStream to byte array
        // for this, copy input stream to output stream
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        byte buf[] = new byte[1024];
        try {

            while (true) {
                int len = r.data.read(buf);
                if (len == -1) {
                    break;
                }
                outstream.write(buf, 0, len);
            }
        } catch (IOException ex) {
            // this must not happen - we have virtual input streams here, no real disk operations that can fail
            Logger.getLogger(VisiCamServer.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        data = outstream.toByteArray();
    }

    /**
     * get cached entry (threadsafe)
     *
     * @return copied Response object for use with gr.ktogias.NanoHTTPD
     */
    public Response getResponse() {
        Response copy = new Response(status, mimeType, new ByteArrayInputStream(data));
        copy.header = (Properties) header.clone();
        return copy;
    }
}
