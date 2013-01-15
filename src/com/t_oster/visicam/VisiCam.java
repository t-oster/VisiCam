package com.t_oster.visicam;

import java.awt.HeadlessException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import javax.swing.JOptionPane;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class VisiCam {

  private static String getLocalIpAddress() throws SocketException {
    
    for (Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces(); nets.hasMoreElements();) {
      NetworkInterface netint = nets.nextElement();
      if (netint.isLoopback() || netint.isPointToPoint() || !netint.isUp())
      {
        continue;
      }
      for (Enumeration<InetAddress> ie = netint.getInetAddresses(); ie.hasMoreElements(); ) {
        InetAddress ia = ie.nextElement();
        if (!ia.isLoopbackAddress())
        {
          String ip = ia.getHostAddress();
          if (!ip.contains(":"))
          {
            return ip;
          }
        }
      }
    }
    return "127.0.0.1";
  }

  private static StatusWindow window;
  
  public static void log(String line)
  {
    if (window != null)
    {
      window.addLog(line);
    }
    else
    {
      System.out.println(line);
    }
  }
  
  public static void error(String error)
  {
    if (window != null)
    {
      JOptionPane.showMessageDialog(window, error, "Error", JOptionPane.OK_OPTION);
    }
    else
    {
      System.err.println(error);
    }
  }
  
  public static void quit()
  {
    //TODO shutdown servers properly
    System.exit(0);
  }
  
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws IOException {

    int port = 8080;
    int broadcastPort = 8888;
    boolean nogui = false;
    for (int i = 0; i < args.length; i++)
    {
      if ("--nogui".equals(args[i]))
      {
        nogui = true;
      }
      else if ("--port".equals(args[i]))
      {
        if (args.length > i+1)
        {
          port = Integer.parseInt(args[i+1]);
          i++;
        }
        else
        {
          System.err.println("--port without argument");
        }
      }
      else if ("--broadcastport".equals(args[i]))
      {
        if (args.length > i+1)
        {
          broadcastPort = Integer.parseInt(args[i+1]);
          i++;
        }
        else
        {
          System.err.println("--broadcastport without argument");
        }
      }
      else if ("--help".equals(args[i]) || "-h".equals(args[i]))
      {
        System.out.println("--nogui\tdisable gui");
        System.out.println("--port <port>\tset port for http");
        System.out.println("--broadcastport\tset broadcast port (udp discovery)");
      }
    }
    //TODO apply cli parameters

    try 
    {
      if (nogui == false)
      {
        window = new StatusWindow();
        window.setVisible(true);
      }
    }
    catch (HeadlessException e)
    {
    }
    
    String uri = "http://" + getLocalIpAddress() + ":" + port;

    try {
      VisiCamServer srv = new VisiCamServer(port, new CameraController());
      BroadcastResponder br = new BroadcastResponder(broadcastPort, uri + "/image");
      br.start();
    } catch (IOException ioe) {
      error("Couldn't start server:\n" + ioe);
      System.exit(-1);
    }
    if (window != null)
    {
      window.setUri(uri);
    }
    else
    {
      System.out.println("Listening on " + uri + ". Hit Enter to stop.\n");
    }
    try {
      System.in.read();
    } catch (Throwable t) {
    };
  }
}
