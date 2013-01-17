/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicam;

import com.google.gson.Gson;
import com.googlecode.javacv.FrameGrabber;
import gr.ktogias.NanoHTTPD;
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
  private int cameraIndex = 0;
  private int inputWidth = 1680;
  private int inputHeight = 1050;
  private int outputWidth = 1680;
  private int outputHeight = 1050;
  
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
    return new Response(HTTP_OK, "application/json", gson.toJson(settings));
  }
  
  private void loadProperties(Properties parms)
  {
    cameraIndex = Integer.parseInt(parms.getProperty("cameraIndex"));
    inputWidth = Integer.parseInt(parms.getProperty("inputWidth"));
    inputHeight = Integer.parseInt(parms.getProperty("inputHeight"));
    outputWidth = Integer.parseInt(parms.getProperty("outputWidth"));
    outputHeight = Integer.parseInt(parms.getProperty("outputHeight"));
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
  
  protected Response serveRawImage() throws IOException, FrameGrabber.Exception
  {
    return serveJpeg(cc.takeSnapshot(cameraIndex, inputWidth, inputHeight));
  }

  @Override
  public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
    try
    {
      if ("GET".equals(method))
      {
        if ("/rawimage".equals(uri))
        {
          return serveRawImage();
        }
        else if("/image".equals(uri))
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
      return new Response(HTTP_INTERNALERROR, MIME_PLAINTEXT, text);
    }
  }

  private Response serveTransformedImage() throws FrameGrabber.Exception, IOException 
  {
    VisiCam.log("Taking Snapshot...");
    BufferedImage img = cc.takeSnapshot(cameraIndex, inputWidth, inputHeight);
    VisiCam.log("Finding markers...");
    int foundMarkers = 0;
    RelativePoint[] currentMarkerPositions = new RelativePoint[lastMarkerPositions.length];
    for (int i = 0; i < currentMarkerPositions.length; i++)
    {
      currentMarkerPositions[i] = cc.findMarker(img, markerSearchfields[i]);
      if (currentMarkerPositions[i] == null)
      {
        currentMarkerPositions[i] = lastMarkerPositions[i];
      }
      else
      {
        foundMarkers ++;
      }
    }
    lastMarkerPositions = currentMarkerPositions;
    VisiCam.log("Found "+foundMarkers+"/"+lastMarkerPositions.length+" markers");
    VisiCam.log("Applying transformation...");
    Response result = serveJpeg(cc.applyHomography(img, lastMarkerPositions, outputWidth, outputHeight));
    VisiCam.log("done");
    return result;
  }
  
  
}
