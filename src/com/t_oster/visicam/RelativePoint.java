package com.t_oster.visicam;

import java.awt.Point;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class RelativePoint 
{
  double x = 0;
  double y = 0;

  public RelativePoint(double x, double y)
  {
    this.x = check(x);
    this.y = check(y);
  }
  
  public RelativePoint(Point subject, int width, int height)
  {
    this.x = check(subject.x / (double) width);
    this.y = check(subject.y / (double) height);
  }
  
  public double getX() {
    return x;
  }

  public void setX(double x) {
    this.x = check(x);
  }

  public double getY() {
    return y;
  }

  public void setY(double y) {
    this.y = check(y);
  }

  public Point toAbsolutePoint(int width, int height)
  {
    return new Point((int) (x*width), (int) (y*height));
  }
  
  protected final double check(double input)
  {
    if (input < 0 || input > 1)
    {
      throw new IllegalArgumentException("Only values between 0 and 1 are allowed");
    }
    return input;
  }
}
