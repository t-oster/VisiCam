package com.t_oster.visicam;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws IOException {

    int port = 8080;
    int broadcastPort = 8888;
    //TODO apply cli parameters

    String uri = "http://" + getLocalIpAddress() + ":" + port;

    try {
      VisiCamServer srv = new VisiCamServer(port, new CameraController());
      BroadcastResponder br = new BroadcastResponder(broadcastPort, uri + "/image");
      br.start();
    } catch (IOException ioe) {
      System.err.println("Couldn't start server:\n" + ioe);
      System.exit(-1);
    }
    System.out.println("Listening on " + uri + ". Hit Enter to stop.\n");
    try {
      System.in.read();
    } catch (Throwable t) {
    };
  }
}
