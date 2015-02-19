package com.t_oster.visicam;

import java.awt.Rectangle;

/**
 * A rectangle, that does only support values from 0 (0%)
 * to 1 (100%)
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class RelativeRectangle extends RelativePoint
{
  private double width = 1;
  private double height = 1;

  public RelativeRectangle(double x, double y, double width, double height)
  {
    super(x, y);
    this.width = check(width);
    this.height = check(height);
  }
  
  public RelativeRectangle(Rectangle subject, int width, int height)
  {
    super(subject.getLocation(), width, height);
    this.width = check(subject.width/ (double) width);
    this.height = check(subject.height/ (double) height);
  }
  
  public double getWidth() {
    return width;
  }

  public void setWidth(double width) {
    this.width = check(width);
  }

  public double getHeight() {
    return height;
  }

  public void setHeight(double height) {
    this.height = check(height);
  }
  
  @Override
  public String toString()
  {
    return "RelativeRect x="+(100*x)+"% y="+(100*y)+"%  w="+(100*width)+"% h="+(100*height)+"%";
  }
  
  public Rectangle toAbsoluteRectangle(int w, int h)
  {
    return new Rectangle((int) (x*w), (int) (y*h), (int) (w*width), (int) (h*height));
  }
}
