/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicam;

import com.google.gson.Gson;
import com.googlecode.javacv.FrameGrabber;
import gr.ktogias.NanoHTTPD;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class VisiCamServer extends NanoHTTPD 
{
  private File config = new File("visicam.conf");
  private CameraController cc;
  private String captureCommand = "";
  private String captureResult = "";
  private int cameraIndex = 0;
  private int inputWidth = 1680;
  private int inputHeight = 1050;
  private int outputWidth = 1680;
  private int outputHeight = 1050;
  private boolean lockInsecureSettings = false;

  // visicamRPiGPU integration start
  private String visicamRPiGPUBinaryPath = "";
  private String visicamRPiGPUMatrixPath = "";
  private String visicamRPiGPUImageOriginalPath = "";
  private String visicamRPiGPUImageProcessedPath = "";
  private Integer visicamRPiGPURefreshSeconds = 0;
  // visicamRPiGPU integration end

  private Gson gson = new Gson();
  
  private RelativeRectangle[] markerSearchfields = new RelativeRectangle[]{
    new RelativeRectangle(0,0,0.2,0.2),
    new RelativeRectangle(0.8,0,0.2,0.2),
    new RelativeRectangle(0,0.8,0.2,0.2),
    new RelativeRectangle(0.8,0.8,0.2,0.2)
  };
  
  private RelativePoint[] lastMarkerPositions = null;
  
  public VisiCamServer(int port, CameraController cc) throws IOException
  {
      super(port, new File("html"));
      this.cc = cc;
      if (config.exists())
      {
        try
        {
          Properties p = new Properties();
          p.load(new FileInputStream(config));
          this.loadProperties(p);
          VisiCam.log("Successfully loaded "+config.getAbsolutePath());
        }
        catch (Exception e)
        {
          VisiCam.log("Error loading "+config.getAbsolutePath());
          VisiCam.log("Default settings will be used.");
        }
      }
      lastMarkerPositions = new RelativePoint[]{
        new RelativePoint(markerSearchfields[0].x + markerSearchfields[0].getWidth()/2, markerSearchfields[0].y + markerSearchfields[0].getHeight()/2),
        new RelativePoint(markerSearchfields[1].x + markerSearchfields[1].getWidth()/2, markerSearchfields[1].y + markerSearchfields[1].getHeight()/2),
        new RelativePoint(markerSearchfields[2].x + markerSearchfields[2].getWidth()/2, markerSearchfields[2].y + markerSearchfields[2].getHeight()/2),
        new RelativePoint(markerSearchfields[3].x + markerSearchfields[3].getWidth()/2, markerSearchfields[3].y + markerSearchfields[3].getHeight()/2)
      };
  }
  
  private Response serveSettings()
  {
    Map<String, Object> settings = new LinkedHashMap<String, Object>();
    settings.put("markerSearchfields", markerSearchfields);
    settings.put("cameraIndex", cameraIndex);
    settings.put("inputWidth", inputWidth);
    settings.put("inputHeight", inputHeight);
    settings.put("outputWidth", outputWidth);
    settings.put("outputHeight", outputHeight);
    settings.put("captureCommand", captureCommand);
    settings.put("captureResult", captureResult);
    settings.put("lockInsecureSettings", lockInsecureSettings);

    // visicamRPiGPU integration start
    settings.put("visicamRPiGPUBinaryPath", visicamRPiGPUBinaryPath);
    settings.put("visicamRPiGPUMatrixPath", visicamRPiGPUMatrixPath);
    settings.put("visicamRPiGPUImageOriginalPath", visicamRPiGPUImageOriginalPath);
    settings.put("visicamRPiGPUImageProcessedPath", visicamRPiGPUImageProcessedPath);
    settings.put("visicamRPiGPURefreshSeconds", visicamRPiGPURefreshSeconds);
    // visicamRPiGPU integration end

    return new Response(HTTP_OK, "application/json", gson.toJson(settings));
  }
  
  private void loadProperties(Properties parms)
  {
    cameraIndex = Integer.parseInt(parms.getProperty("cameraIndex"));
    inputWidth = Integer.parseInt(parms.getProperty("inputWidth"));
    inputHeight = Integer.parseInt(parms.getProperty("inputHeight"));
    outputWidth = Integer.parseInt(parms.getProperty("outputWidth"));
    outputHeight = Integer.parseInt(parms.getProperty("outputHeight"));
    lockInsecureSettings = Boolean.parseBoolean(parms.getProperty("lockInsecureSettings"));
    captureCommand = parms.getProperty("captureCommand");
    captureResult = parms.getProperty("captureResult");

    // visicamRPiGPU integration start
    visicamRPiGPUBinaryPath = parms.getProperty("visicamRPiGPUBinaryPath");
    visicamRPiGPUMatrixPath = parms.getProperty("visicamRPiGPUMatrixPath");
    visicamRPiGPUImageOriginalPath = parms.getProperty("visicamRPiGPUImageOriginalPath");
    visicamRPiGPUImageProcessedPath = parms.getProperty("visicamRPiGPUImageProcessedPath");
    visicamRPiGPURefreshSeconds = Integer.parseInt(parms.getProperty("visicamRPiGPURefreshSeconds"));
    // visicamRPiGPU integration end

    for (int i = 0; i < markerSearchfields.length; i++)
    {
      markerSearchfields[i].setX(Double.parseDouble(parms.getProperty("markerSearchfields["+i+"][x]")));
      markerSearchfields[i].setY(Double.parseDouble(parms.getProperty("markerSearchfields["+i+"][y]")));
      markerSearchfields[i].setWidth(Double.parseDouble(parms.getProperty("markerSearchfields["+i+"][width]")));
      markerSearchfields[i].setHeight(Double.parseDouble(parms.getProperty("markerSearchfields["+i+"][height]")));
    }
  }
  
  private Response updateSettings(Properties parms)
  {
    try 
    {
      if (lockInsecureSettings)
      {
          // if set, do not allow changing any settings that could cause command execution or data leakage
          // do not accept new values from parms, overwrite them with previous stored values
          parms.setProperty("lockInsecureSettings", Boolean.toString(lockInsecureSettings));
          parms.setProperty("captureCommand", captureCommand);
          parms.setProperty("captureResult", captureResult);

          // visicamRPiGPU integration start
          parms.setProperty("visicamRPiGPUBinaryPath", visicamRPiGPUBinaryPath);
          parms.setProperty("visicamRPiGPUMatrixPath", visicamRPiGPUMatrixPath);
          parms.setProperty("visicamRPiGPUImageOriginalPath", visicamRPiGPUImageOriginalPath);
          parms.setProperty("visicamRPiGPUImageProcessedPath", visicamRPiGPUImageProcessedPath);
          parms.setProperty("visicamRPiGPURefreshSeconds", visicamRPiGPURefreshSeconds.toString());
          // visicamRPiGPU integration end
      }
      parms.store(new FileOutputStream(config), "VisiCam Configuration");
    } catch (IOException ex) 
    {
      VisiCam.log("Could not save settings to "+config.getAbsolutePath());
      VisiCam.log("Settings will be reset after restart");
    }
    loadProperties(parms);
    return new Response(HTTP_OK, "application/json", "true");
  }
  
  private Response serveJpeg(BufferedImage img) throws IOException
  {
    return new Response(HTTP_OK, "image/jpg", cc.toJpegStream(img));
  }
  
  // serve the raw input image with green X at the marker locations and red rectangles in the searchfields
  protected Response serveRawImage() throws IOException, FrameGrabber.Exception, InterruptedException
  {
      try
      {
        BufferedImage img=cc.takeSnapshot(cameraIndex, inputWidth, inputHeight, captureCommand, captureResult);
        RelativePoint[] currentMarkerPositions = cc.findMarkers(img, markerSearchfields);
        Graphics2D g=img.createGraphics();
          for (int i = 0; i < currentMarkerPositions.length; i++)
          {
            int fieldX = (int) (markerSearchfields[i].getX()*img.getWidth());
            int fieldW = (int) (markerSearchfields[i].getWidth()*img.getWidth());
            int fieldY = (int) (markerSearchfields[i].getY()*img.getHeight());
            int fieldH = (int) (markerSearchfields[i].getHeight()*img.getHeight());
            g.setStroke(new BasicStroke(4));
            g.setColor(Color.RED);
            g.drawRect(fieldX, fieldY, fieldW, fieldH);
            if (currentMarkerPositions[i] != null)
            {
              int x = (int) (currentMarkerPositions[i].getX()*img.getWidth());
              int y = (int) (currentMarkerPositions[i].getY()*img.getHeight());
              g.setColor(Color.GREEN);
              g.drawLine(x-100, y-100, x+100, y+100);
              g.drawLine(x-100, y+100, x+100, y-100);
            }
          }
        return serveJpeg(img);
      }
      catch (Exception e)
      {
        String txt = "Error: "+e.getMessage();
        VisiCam.error(txt);
        //return servePlaintextError(txt);
        // unable to fetch the image from the camera, or something similarly strange happened.
        // Do not serve a plaintext error message because the VisiCam web-configuration frontend cannot display it
        return serveJpeg(cc.getDummyImage("html/error.jpg",txt));
      }
  }

  // serve a HTTP 500 internal error with a plaintext message
  // (a client like VisiCut or a browser can then display the message to the user)
  public Response servePlaintextError(String errorText) {
    return new Response(HTTP_INTERNALERROR, MIME_PLAINTEXT, errorText);
  }

  @Override
  public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
    try
    {
      if ("GET".equals(method))
      {
        if (uri.startsWith("/rawimage"))
        {
          return serveRawImage();
        }
        else if(uri.startsWith("/image"))
        {
          return serveTransformedImage();
        }
        else if ("/settings".equals(uri))
        {
          return serveSettings();
        }
      }
      else if ("POST".equals(method) && "/settings".equals(uri))
      {
        return updateSettings(parms);
      }
      return super.serve(uri, method, header, parms, files);
    }
    catch (Exception e)
    {
      String text = "Exception: "+e.getMessage();
      for(StackTraceElement s: e.getStackTrace())
      {
        text += s.getClassName()+"."+s.getMethodName()+" in "+s.getFileName()+" line "+s.getLineNumber();
        text += "\n";
      }
      return servePlaintextError(text);
    }
  }

  private Response serveTransformedImage() throws FrameGrabber.Exception, IOException, InterruptedException 
  {
   try {
       for (int retries=0; retries < 3; retries++) {
          VisiCam.log("Taking Snapshot...");
          BufferedImage img = cc.takeSnapshot(cameraIndex, inputWidth, inputHeight, captureCommand, captureResult);
          VisiCam.log("Finding markers...");
          RelativePoint[] currentMarkerPositions = cc.findMarkers(img, markerSearchfields);
          boolean markerError=false;
          for (int i = 0; i < currentMarkerPositions.length; i++)
          {
            if (currentMarkerPositions[i] == null)
            {
              String[] positionNames= {"top-left","top-right","bottom-left","bottom-right"};
              String markerErrorMsg="Cannot find marker " + positionNames[i];
              VisiCam.log(markerErrorMsg + " - retrying up to 3x.");
              if (retries == 2) {
                  throw new Exception(markerErrorMsg + "\nIs the lasercutter open and the camera calibrated correctly?"); // TODO I18N for error messages, but how?
              }
              markerError=true;
            }
          }
          if (markerError) {
              continue;
          }
          VisiCam.log("Applying transformation...");
          Response result = serveJpeg(cc.applyHomography(img, currentMarkerPositions, outputWidth, outputHeight));
          VisiCam.log("done");
          return result;
        }
        VisiCam.log("Not all markers found! Giving up.");
        throw new Exception("Not enough markers found.");
   }
   catch (Exception e) {
       VisiCam.error(e.getMessage());
       return servePlaintextError("VisiCam Error: "+e.getMessage());
       //return serveJpeg(cc.getDummyImage("html/error.jpg", "Error:"+e.getMessage()));
   }
  }
  
  
}
