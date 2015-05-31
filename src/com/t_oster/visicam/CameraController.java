package com.t_oster.visicam;

import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.OpenCVFrameGrabber;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvPoint3D32f;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class CameraController 
{
  private volatile CvMat homographyMatrix = null;
  private volatile Boolean synchronizedCamera = false;

  public InputStream toJpegStream(final BufferedImage img) throws IOException
  {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", bos);
    return new ByteArrayInputStream(bos.toByteArray());
  }

  public synchronized BufferedImage takeSnapshot(int cameraIndex, int width, int height, String command, String path, boolean visicamRPiGPUEnabled, String visicamRPiGPUImageOriginalPath) throws Exception, IOException, InterruptedException
  {
    BufferedImage result;

    // visicamRPiGPU integration start
    if (visicamRPiGPUEnabled)
    {
        // Default return value
        result = null;

        // Create file object
        File originalImageFile = new File(visicamRPiGPUImageOriginalPath);

        // Check if file exists
        if (originalImageFile.exists() && !originalImageFile.isDirectory())
        {
            // Lock file
            FileChannel originalImageChannel = new RandomAccessFile(originalImageFile, "rw").getChannel();
            FileLock originalImageLock = originalImageChannel.lock();

            // Read file data into memory
            Path originalImagePath = Paths.get(visicamRPiGPUImageOriginalPath);
            byte[] originalImageFileData = Files.readAllBytes(originalImagePath);

            // Unlock file
            originalImageLock.release();

            // Create input stream from memory file data
            ByteArrayInputStream originalImageByteInputStream = new ByteArrayInputStream(originalImageFileData);

            // Build result from input stream
            result = ImageIO.read(originalImageByteInputStream);
        }
    }
    // visicamRPiGPU integration end
    else if (command == null || "".equals(command))
      {
        synchronized (synchronizedCamera)
        {
            OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(cameraIndex);
            grabber.setImageHeight(height);
            grabber.setImageWidth(width);
            grabber.start();
            IplImage img = grabber.grab();
            result = img.getBufferedImage();
            grabber.stop();
        }
      }
      else
      {
        synchronized (synchronizedCamera)
        {
            Runtime r = Runtime.getRuntime();
            command = command.replace("%i", ""+cameraIndex).replace("%w", ""+width).replace("%h", ""+height).replace("%f", path);
            Process pr = r.exec(command);
            InputStream os = pr.getErrorStream();
            int b;
            String errors = "";
            while  ((b = os.read()) != -1)
            {
              errors += ""+(char)b;
            }
            pr.waitFor();
            if (pr.exitValue() != 0 && !"".equals(errors))
            {
              throw new Exception(errors);
            }
            result = ImageIO.read(new File(path));
        }
    }
  return result;
  }

  public BufferedImage getDummyImage(String filename, String txt) throws IOException {
	BufferedImage result = ImageIO.read(new File(filename));
        Graphics2D g = result.createGraphics();
        g.setFont(g.getFont().deriveFont(72));
        double w = g.getFontMetrics().stringWidth(txt);
        int textHeight=2*g.getFontMetrics().getHeight();
        //g.scale((result.getWidth()-200)/w, (result.getWidth()-200)/w);
        g.clearRect(100, 100, result.getWidth()-200, textHeight);
        g.drawString(txt, 100, 100+textHeight);
        float s = (float) ((result.getWidth()-200)/w);
        g.scale(s,s);
        g.clearRect((int) (100/s), (int) (100/s), result.getWidth()-200, textHeight);
        g.drawString(txt, 100/s, 100/s+textHeight/2);
	return result;
  }
  
  public RelativePoint findMarker(BufferedImage input, RelativeRectangle roi)
  {
    Rectangle abs = roi.toAbsoluteRectangle(input.getWidth(), input.getHeight());
    IplImage in = IplImage.createFrom(input.getSubimage(abs.x, abs.y, abs.width, abs.height));
    IplImage gray = IplImage.create(in.width(), in.height(), in.depth(), 1);
    cvCvtColor(in, gray, CV_BGR2GRAY);
    cvErode(gray, gray, null, 1);
    cvErode(gray, gray, null, 1);
    cvDilate(gray, gray, null, 1);
    cvDilate(gray, gray, null, 1);
    cvErode(gray, gray, null, 1);
    cvDilate(gray, gray, null, 1);
    CvMemStorage storage = cvCreateMemStorage(0);
    double minDist = 10;
    double cannyThreshold = 50;
    double minVotes = 15;
    int minRad = 5;
    int maxRad = 25;
    CvSeq circles = cvHoughCircles(gray, storage, CV_HOUGH_GRADIENT, 1, minDist, cannyThreshold, minVotes, minRad, maxRad);
    if (circles.total() >= 1)
    {
      CvPoint3D32f point = new CvPoint3D32f(cvGetSeqElem(circles, 0));
      CvPoint center = cvPointFrom32f(new CvPoint2D32f(point.x(), point.y()));
      RelativePoint result = new RelativePoint((center.x()+abs.x)/(double)input.getWidth(), (center.y()+abs.y)/(double)input.getHeight());
      return result;
    }
    return null;
  }
  
  // find all markers in searchfields, returns an array with RelativePoint entries (or null entry if marker not found)
  public RelativePoint[] findMarkers(BufferedImage img, RelativeRectangle[] markerSearchfields)
  {
      RelativePoint[] currentMarkerPositions = new RelativePoint[markerSearchfields.length];
      for (int i = 0; i < markerSearchfields.length; i++)
      {
        currentMarkerPositions[i] = findMarker(img, markerSearchfields[i]);
        // VisiCam.log("Marker " + i + ":"  + currentMarkerPositions[i] + " (in rectangle: " + markerSearchfields[i] +  ")");
      }
      return currentMarkerPositions;
  }
  
  public BufferedImage applyHomography(BufferedImage img)
  {
    if (homographyMatrix != null)
    {
        synchronized (homographyMatrix)
        {
            IplImage in = IplImage.createFrom(img);
            cvWarpPerspective(in, in, homographyMatrix);
            return in.getBufferedImage();
        }
    }

    return null;
  }

  public void updateHomographyMatrix(BufferedImage img, RelativePoint[] markerPositions, double ouputWidth, double outputHeight, boolean visicamRPiGPUEnabled, String visicamRPiGPUMatrixPath) throws FileNotFoundException, IOException
  {
    CvMat src = CvMat.create(markerPositions.length, 1, CV_32FC(2));

    for (int i = 0; i < markerPositions.length; i++)
    {
      Point p = markerPositions[i].toAbsolutePoint(img.getWidth(), img.getHeight());
      src.put(i, 0, 0, p.x);
      src.put(i, 0, 1, p.y);
    }

    CvMat dst = CvMat.create(4, 1, CV_32FC(2));
    dst.put(0, 0, 0, 0);
    dst.put(0, 0, 1, 0);
    dst.put(1, 0, 0, img.getWidth());
    dst.put(1, 0, 1, 0);
    dst.put(2, 0, 0, 0);
    dst.put(2, 0, 1, img.getHeight());
    dst.put(3, 0, 0, img.getWidth());
    dst.put(3, 0, 1, img.getHeight());

    CvMat localHomographyMatrix = CvMat.create(3, 3);
    cvFindHomography(src, dst, localHomographyMatrix, CV_RANSAC, 1, null);

    // Write matrix values to file for visicamRPiGPU if needed
    if (visicamRPiGPUEnabled)
    {
        // Create file object
        File matrixOutputFile = new File(visicamRPiGPUMatrixPath);

        // Check if file exists
        // Lock file
        FileChannel matrixOutputChannel = new RandomAccessFile(matrixOutputFile, "rw").getChannel();
        FileLock matrixOutputLock = matrixOutputChannel.lock();

        // Write matrix values to file
        PrintWriter matrixOutputWriter = new PrintWriter(matrixOutputFile);

        // Iterate over matrix and write data
        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 3; ++j)
            {
                float matrixValue = (float)(localHomographyMatrix.get(i, j));
                matrixOutputWriter.println(matrixValue);
            }
        }

        // Close writer
        matrixOutputWriter.flush();
        matrixOutputWriter.close();

        // Unlock file
        matrixOutputLock.release();
    }

    // This looks weird, but homographyMatrix must not be null for synchronized access
    // If it is null, no need to care for synchronized access at all
    if (homographyMatrix != null)
    {
        synchronized (homographyMatrix)
        {
            homographyMatrix = localHomographyMatrix;
        }
    }
    else
    {
        homographyMatrix = localHomographyMatrix;
    }
  }

  public CvMat getHomographyMatrix()
  {
    // homographyMatrix must not be null for synchronized access
    if (homographyMatrix != null)
    {
        synchronized (homographyMatrix)
        {
            return homographyMatrix;
        }
    }

    return null;
  }
}
