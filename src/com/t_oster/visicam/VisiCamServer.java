/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicam;

import com.googlecode.javacv.FrameGrabber;
import gr.ktogias.NanoHTTPD;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class VisiCamServer extends NanoHTTPD 
{
  //private File config = new File("visicam.conf");
  private CameraController cc;
  private int cameraIndex = 0;
  private int inputWidth = 1680;
  private int inputHeight = 1050;
  private int outputWidth = 1680;
  private int outputHeight = 1050;
  
  private RelativeRectangle[] markerSearchfields = new RelativeRectangle[]{
    new RelativeRectangle(0,0,0.2,0.2),
    new RelativeRectangle(0.8,0,0.2,0.2),
    new RelativeRectangle(0,0.8,0.2,0.2),
    new RelativeRectangle(0.8,0.8,0.2,0.2)
  };
  
  private RelativePoint[] lastMarkerPositions = new RelativePoint[]{
    new RelativePoint(0.1,0.1),
    new RelativePoint(0.9,0.1),
    new RelativePoint(0.1,0.9),
    new RelativePoint(0.9,0.9)
  };
  
  public VisiCamServer(int port, CameraController cc) throws IOException
  {
    	super(port, new File("html"));
      this.cc = cc;
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
      }
      else if ("POST".equals(method) && "/configPage.html".equals(uri))
      {
        //TODO update config
        return super.serve("/configPage.html", "get", header, parms, files);
      }
      return super.serve(uri, method, header, parms, files);
    }
    catch (Exception e)
    {
      return new Response(HTTP_INTERNALERROR, MIME_PLAINTEXT, e.getMessage());
    }
  }

  private Response serveTransformedImage() throws FrameGrabber.Exception, IOException 
  {
    BufferedImage img = cc.takeSnapshot(cameraIndex, inputWidth, inputHeight);
    RelativePoint[] currentMarkerPositions = new RelativePoint[lastMarkerPositions.length];
    for (int i = 0; i < currentMarkerPositions.length; i++)
    {
      currentMarkerPositions[i] = cc.findMarker(img, markerSearchfields[i]);
      if (currentMarkerPositions[i] == null)
      {
        currentMarkerPositions[i] = lastMarkerPositions[i];
      }
    }
    lastMarkerPositions = currentMarkerPositions;
    return serveJpeg(cc.applyHomography(img, lastMarkerPositions, outputWidth, outputHeight));
  }
  
  
}
