package com.t_oster.visicam;

import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class CameraController 
{
  
  public InputStream toJpegStream(final BufferedImage img) throws IOException
  {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", bos);
    return new ByteArrayInputStream(bos.toByteArray());
  }
  
  public BufferedImage takeSnapshot(int cameraIndex) throws Exception
  {
    OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(cameraIndex);
    grabber.setImageHeight(1050);
    grabber.setImageWidth(1680);
    grabber.start();
    IplImage img = grabber.grab();
    BufferedImage result = img.getBufferedImage();
    grabber.stop();
    return result;
  }
  
  public RelativePoint findMarker(BufferedImage input, RelativeRectangle roi)
  {
    throw new RuntimeException("Not implemented");
  }
  
  public BufferedImage applyHomography(BufferedImage img, RelativePoint[] markerPositions, double ouputWidth, double outputHeight)
  {
    throw new RuntimeException("Not implemented");
  }
  
}
