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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.management.ManagementFactory;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
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
  private Integer refreshSeconds = 30;
  private long lastSuccessfulRefreshTime = 0;
  private long lastRequestTime = System.nanoTime();
  private boolean lockInsecureSettings = false;

  // visicamRPiGPU integration start
  private Integer visicamRPiGPUInactivitySeconds = 600;
  private String visicamRPiGPUBinaryPath = "";
  private String visicamRPiGPUMatrixPath = "";
  private String visicamRPiGPUImageOriginalPath = "";
  private String visicamRPiGPUImageProcessedPath = "";

  private Thread visicamRPiGPUInactivityThread = null;
  private final Boolean visicamRPiGPUFileLockSynchronization = false;     // Dummy variable only used for file lock synchronization
  private boolean visicamRPiGPUEnabled = false;
  private int visicamRPiGPUPid = -10;           // Strange default values for PIDs, but -1, 0 and 1, other positive numbers
  private int visicamPid = -10;                 // are all assigned, would return wrong results in checking if process runs
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

    // visicamRPiGPU integration start
    visicamRPiGPUInactivityThread = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                while (true)
                {
                    if ((System.nanoTime() - lastRequestTime) >= (visicamRPiGPUInactivitySeconds * 1000000000L))
                    {
                        Runtime runtime = Runtime.getRuntime();
                        File visicamRPiGPUbinaryFile = new File(visicamRPiGPUBinaryPath);

                        // Check if visicamRPiGPU is already running with saved PID
                        Process checkAlive = runtime.exec("kill -0 " + visicamRPiGPUPid);
                        checkAlive.waitFor();

                        // visicamRPiGPU is running with visicamRPiGPUPid
                        if (checkAlive.exitValue() == 0)
                        {
                            VisiCam.log("visicamRPiGPU instance is killed because of inactivity...");
                            Process killAll = runtime.exec("pkill -9 -f " + "^.*?" + visicamRPiGPUbinaryFile.getName() + "[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]].*?[[:space:]].*?[[:space:]].*?$");
                            visicamRPiGPUPid = -10;
                        }
                    }

                    Thread.sleep(refreshSeconds * 1000);
                }
            }
            catch (Exception e)
            {
                VisiCam.log("visicamRPiGPU inactivity thread exception: " + e.getMessage());
            }
        }
    });

    visicamRPiGPUInactivityThread.start();
    // visicamRPiGPU integration end
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
    settings.put("refreshSeconds", refreshSeconds);
    settings.put("captureCommand", captureCommand);
    settings.put("captureResult", captureResult);
    settings.put("lockInsecureSettings", lockInsecureSettings);

    // visicamRPiGPU integration start
    settings.put("visicamRPiGPUInactivitySeconds", visicamRPiGPUInactivitySeconds);
    settings.put("visicamRPiGPUBinaryPath", visicamRPiGPUBinaryPath);
    settings.put("visicamRPiGPUMatrixPath", visicamRPiGPUMatrixPath);
    settings.put("visicamRPiGPUImageOriginalPath", visicamRPiGPUImageOriginalPath);
    settings.put("visicamRPiGPUImageProcessedPath", visicamRPiGPUImageProcessedPath);
    // visicamRPiGPU integration end

    return new Response(HTTP_OK, "application/json", gson.toJson(settings));
  }
  
  private synchronized void loadProperties(Properties parms)
  {
    boolean baseSettingsChanged = (inputWidth != Integer.parseInt(parms.getProperty("inputWidth"))
                                   || inputHeight != Integer.parseInt(parms.getProperty("inputHeight"))
                                   || outputWidth != Integer.parseInt(parms.getProperty("outputWidth"))
                                   || outputHeight != Integer.parseInt(parms.getProperty("outputHeight"))
                                   || refreshSeconds != Integer.parseInt(parms.getProperty("refreshSeconds")));

    cameraIndex = Integer.parseInt(parms.getProperty("cameraIndex"));
    inputWidth = Integer.parseInt(parms.getProperty("inputWidth"));
    inputHeight = Integer.parseInt(parms.getProperty("inputHeight"));
    outputWidth = Integer.parseInt(parms.getProperty("outputWidth"));
    outputHeight = Integer.parseInt(parms.getProperty("outputHeight"));
    refreshSeconds = Integer.parseInt(parms.getProperty("refreshSeconds"));
    lockInsecureSettings = Boolean.parseBoolean(parms.getProperty("lockInsecureSettings"));
    captureCommand = parms.getProperty("captureCommand");
    captureResult = parms.getProperty("captureResult");

    // Ensure that there are positive values for some integer values
    if (inputWidth <= 0)
    {
        inputWidth = 1680;
    }

    if (inputHeight <= 0)
    {
        inputHeight = 1050;
    }

    if (outputWidth <= 0)
    {
        outputWidth = 1680;
    }

    if (outputHeight <= 0)
    {
        outputHeight = 1050;
    }

    if (refreshSeconds <= 0)
    {
        refreshSeconds = 30;
    }

    // visicamRPiGPU integration start
    String visicamRPiGPUBinaryPathPrevious = visicamRPiGPUBinaryPath;
    boolean visicamRPiGPUEnabledTmp = visicamRPiGPUEnabled;
    boolean visicamRPiGPUEnabledOptionsChanged = (baseSettingsChanged ||
                                                  !visicamRPiGPUBinaryPath.equals(parms.getProperty("visicamRPiGPUBinaryPath")) ||
                                                  !visicamRPiGPUMatrixPath.equals(parms.getProperty("visicamRPiGPUMatrixPath")) ||
                                                  !visicamRPiGPUImageOriginalPath.equals(parms.getProperty("visicamRPiGPUImageOriginalPath")) ||
                                                  !visicamRPiGPUImageProcessedPath.equals(parms.getProperty("visicamRPiGPUImageProcessedPath")));

    visicamRPiGPUInactivitySeconds = Integer.parseInt(parms.getProperty("visicamRPiGPUInactivitySeconds"));

    if (visicamRPiGPUInactivitySeconds <= 0)
    {
        visicamRPiGPUInactivitySeconds = 600;
    }

    visicamRPiGPUBinaryPath = parms.getProperty("visicamRPiGPUBinaryPath");
    visicamRPiGPUMatrixPath = parms.getProperty("visicamRPiGPUMatrixPath");
    visicamRPiGPUImageOriginalPath = parms.getProperty("visicamRPiGPUImageOriginalPath");
    visicamRPiGPUImageProcessedPath = parms.getProperty("visicamRPiGPUImageProcessedPath");

    visicamRPiGPUEnabled = (visicamRPiGPUInactivitySeconds > 0 && !visicamRPiGPUBinaryPath.isEmpty() && !visicamRPiGPUMatrixPath.isEmpty() &&
                            !visicamRPiGPUImageOriginalPath.isEmpty() && !visicamRPiGPUImageProcessedPath.isEmpty());

    // On startup no previous path is set, fallback to use current path if available, so current instances might be killed correctly
    if (visicamRPiGPUBinaryPathPrevious.isEmpty() && !visicamRPiGPUBinaryPath.isEmpty())
    {
        visicamRPiGPUBinaryPathPrevious = visicamRPiGPUBinaryPath;
    }

    // Since visicamRPiGPU currently is only designed for Raspbian on the Raspberry Pi 2 (hardware acceleration)
    // this platform dependency with bash commands is not an issue at all
    Runtime runtime = Runtime.getRuntime();
    File visicamRPiGPUbinaryFile = new File(visicamRPiGPUBinaryPath);
    File visicamRPiGPUbinaryFilePrevious = new File(visicamRPiGPUBinaryPathPrevious);

    try
    {
        // Check if visicamRPiGPU was turned off in settings
        if (visicamRPiGPUEnabledTmp && !visicamRPiGPUEnabled)
        {
            VisiCam.log("visicamRPiGPU was disabled in settings, kill all visicamRPiGPU instances...");
            Process killAll = runtime.exec("pkill -9 -f " + "^.*?" + visicamRPiGPUbinaryFilePrevious.getName() + "[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]].*?[[:space:]].*?[[:space:]].*?$");
            killAll.waitFor();
            visicamRPiGPUPid = -10;
        }
        // If visicamRPiGPU should be enabled, check if it is running with stored PID
        else if (visicamRPiGPUEnabled)
        {
            // Check if visicamRPiGPU is already running with saved PID
            Process checkAlive = runtime.exec("kill -0 " + visicamRPiGPUPid);
            checkAlive.waitFor();

            // visicamRPiGPU is not running with visicamRPiGPUPid OR needs to be restarted
            if (checkAlive.exitValue() != 0 || visicamRPiGPUEnabledOptionsChanged)
            {
                VisiCam.log("visicamRPiGPU is not running with correct settings, kill all visicamRPiGPU instances...");
                Process killAll = runtime.exec("pkill -9 -f " + "^.*?" + visicamRPiGPUbinaryFilePrevious.getName() + "[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]].*?[[:space:]].*?[[:space:]].*?$");
                killAll.waitFor();
                visicamRPiGPUPid = -10;

                if (visicamRPiGPUbinaryFile.exists() && !visicamRPiGPUbinaryFile.isDirectory())
                {
                    VisiCam.log("Starting new visicamRPiGPU instance...");

                    // Detect own visicam pid
                    if (visicamPid == -10)
                    {
                        String pidMachineString = ManagementFactory.getRuntimeMXBean().getName();
                        int seperatorIndex = pidMachineString.indexOf('@');
                        visicamPid = Integer.parseInt(pidMachineString.substring(0, seperatorIndex));
                    }

                    // Usual process does not provide pid, need to use a trick here, since Process is actually a UNIXProcess (simple casting not possible?)
                    String visicamRPiGPUCommand = visicamRPiGPUbinaryFile + " " + outputWidth + " " + outputHeight + " " + refreshSeconds + " " + visicamPid + " " + visicamRPiGPUMatrixPath + " " + visicamRPiGPUImageProcessedPath + " " + visicamRPiGPUImageOriginalPath;
                    Process startNew = (runtime.exec(visicamRPiGPUCommand));
                    Field pidField = startNew.getClass().getDeclaredField("pid");
                    pidField.setAccessible(true);
                    visicamRPiGPUPid = pidField.getInt(startNew);
                }
                else
                {
                    VisiCam.log("visicamRPiGPUbinaryFile does not exist, can not create new visicamRPiGPU instance...");
                }
            }
        }
    }
    catch (Exception e)
    {
        VisiCam.error(e.getMessage());
    }
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
        // Set request timer
        lastRequestTime = System.nanoTime();

        // visicamRPiGPU integration start
        if (visicamRPiGPUEnabled)
        {
            // Reload current settings, ensure that visicamRPiGPU process is running correctly
            if (config.exists())
            {
                Properties p = new Properties();
                FileInputStream inputStream = new FileInputStream(config);
                p.load(inputStream);
                inputStream.close();
                loadProperties(p);
            }
        }
        // visicamRPiGPU integration end

        BufferedImage img=cc.takeSnapshot(cameraIndex, inputWidth, inputHeight, captureCommand, captureResult, visicamRPiGPUEnabled, visicamRPiGPUImageOriginalPath);
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

  private Response serveTransformedImage()
  {
   try
   {
       // Avoid exception at initialization
       if (cc == null)
       {
            return null;
       }

       // Set request timer
       lastRequestTime = System.nanoTime();

       // Check if need to refresh homography matrix because refreshSeconds expired since last refresh time
       // Or because application just started and homography matrix is not ready yet
       if ((System.nanoTime() - lastSuccessfulRefreshTime) >= (refreshSeconds * 1000000000L) || cc.getHomographyMatrix() == null)
       {
            // Update last refresh timer
            // If everything runs correctly, wait refreshSeconds for next run
            // Will be reset to 0 on exception in thread to get an instant refresh on next request
            updateLastRefreshTime();

            // visicamRPiGPU integration start
            if (visicamRPiGPUEnabled)
            {
                // Reload current settings, ensure that visicamRPiGPU process is running correctly
                if (config.exists())
                {
                    Properties p = new Properties();
                    FileInputStream inputStream = new FileInputStream(config);
                    p.load(inputStream);
                    inputStream.close();
                    loadProperties(p);
                }
            }
            // visicamRPiGPU integration end

            // If this is true, it will run synchronously, otherwise asynchronously
            refreshHomography((cc.getHomographyMatrix() == null));
       }

       // Prepare response
       Response result = null;

       // visicamRPiGPU integration start
       if (visicamRPiGPUEnabled)
       {
          // Create file object
          File processedImageFile = new File(visicamRPiGPUImageProcessedPath);

          // Check if file exists
          if (processedImageFile.exists() && !processedImageFile.isDirectory())
          {
            // Variable to store result
            byte[] processedImageFileData = null;

            // Synchronized access because file lock may only be acquired once by this Java application
            synchronized (visicamRPiGPUFileLockSynchronization)
            {
                // Lock file
                FileChannel processedImageChannel = new RandomAccessFile(processedImageFile, "rw").getChannel();
                FileLock processedImageLock = processedImageChannel.lock();

                // Read file data into memory
                Path processedImagePath = Paths.get(visicamRPiGPUImageProcessedPath);
                processedImageFileData = Files.readAllBytes(processedImagePath);

                // Unlock, close file
                processedImageLock.release();
                processedImageChannel.close();
            }

            // Create input stream from memory file data
            ByteArrayInputStream processedImageByteInputStream = new ByteArrayInputStream(processedImageFileData);

            // Build result from input stream
            result = new Response(HTTP_OK, "image/jpg", processedImageByteInputStream);
          }
          else
          {
            throw new Exception("visicamRPiGPU: File at visicamRPiGPUImageProcessedPath does not exist!");
          }
       }
       // visicamRPiGPU integration end
       else // Default behaviour
       {
          BufferedImage img = cc.takeSnapshot(cameraIndex, inputWidth, inputHeight, captureCommand, captureResult, visicamRPiGPUEnabled, visicamRPiGPUImageOriginalPath);

          // Check if img is null, exception
          if (img == null)
          {
              throw new Exception("Image is null before applying homography.");
          }

          // Homography matrix must be set at this point
          result = serveJpeg(cc.applyHomography(img));
       }

       return result;
   }
   catch (Exception e)
   {
       VisiCam.error(e.getMessage());
       return servePlaintextError("VisiCam Error: "+e.getMessage());
       //return serveJpeg(cc.getDummyImage("html/error.jpg", "Error:"+e.getMessage()));
   }
  }

  private synchronized void updateLastRefreshTime()
  {
    lastSuccessfulRefreshTime = System.nanoTime();
  }

  private synchronized void resetLastRefreshTime()
  {
    lastSuccessfulRefreshTime = 0;
  }

  private void refreshHomography(boolean synchronous) throws InterruptedException
  {
    Thread refreshHomographyThread = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                // Log message and variables
                VisiCam.log("Refresh started...");
                BufferedImage img = null;
                RelativePoint[] currentMarkerPositions = null;

                // Loop for marker detection
                for (int retries = 0; retries < 5; retries++)
                {
                  // VisiCam.log("Getting Snapshot...");
                  img = cc.takeSnapshot(cameraIndex, inputWidth, inputHeight, captureCommand, captureResult, visicamRPiGPUEnabled, visicamRPiGPUImageOriginalPath);

                  // If too much retries needed, throw exception
                  if (retries >= 4)
                  {
                      throw new Exception("Give up refreshing, is the lasercutter open and the camera configured correctly?"); // TODO I18N for error messages, but how?
                  }

                  // Check if img is not null, otherwise retry
                  if (img == null)
                  {
                      VisiCam.log("Image is null - retrying up to 4x.");
                      continue;
                  }

                  // VisiCam.log("Finding markers...");
                  currentMarkerPositions = cc.findMarkers(img, markerSearchfields);

                  // Check if all markers were found
                  boolean allMarkersFound = true;
                  for (int i = 0; i < currentMarkerPositions.length; i++)
                  {
                    if (currentMarkerPositions[i] == null)
                    {
                      allMarkersFound = false;
                      String[] positionNames = {"top-left","top-right","bottom-left","bottom-right"};
                      String markerErrorMsg = "Cannot find marker " + positionNames[i];
                      VisiCam.log(markerErrorMsg + " - retrying up to 4x.");
                    }
                  }

                  // If too much retries needed, throw exception
                  if (retries >= 4)
                  {
                      throw new Exception("Give up refreshing, is the lasercutter open and the camera configured correctly?"); // TODO I18N for error messages, but how?
                  }

                  // If all markers were found, stop loop, set timestamp. Otherwise continue trying.
                  if (allMarkersFound)
                  {
                      // VisiCam.log("Markers detected successfully...");
                      break;
                  }
                }

                // Update homography matrix with new marker positions
                // VisiCam.log("Updating homography matrix...");
                cc.updateHomographyMatrix(img, currentMarkerPositions, outputWidth, outputHeight, visicamRPiGPUEnabled, visicamRPiGPUMatrixPath);

                // Log message and set timer
                VisiCam.log("Refreshed successfully...");
                updateLastRefreshTime();
            }
            catch (Exception e)
            {
                resetLastRefreshTime();
                VisiCam.error(e.getMessage());
            }
        }
    });

    // Start thread
    refreshHomographyThread.start();

    // Wait for thread to end if synchronous
    if (synchronous)
    {
        refreshHomographyThread.join();
    }
  }
}
