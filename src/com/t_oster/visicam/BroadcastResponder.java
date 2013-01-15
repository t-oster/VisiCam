package com.t_oster.visicam;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * This class responds to UDP broadcast on the given port which contain
 * "VisiCamDiscover" with the given uri string
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class BroadcastResponder extends Thread {

  private String responseUri;
  private int port;
  
  public BroadcastResponder(int port, String responseUri)
  {
    this.port = port;
    this.responseUri = responseUri;
  }
  
  @Override
  public void run() {
    try {
      //Keep a socket open to listen to all the UDP trafic that is destined for this port
      DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
      socket.setBroadcast(true);

      while (true) {
        VisiCam.log(">>>Ready to receive broadcast packets!");

        //Receive a packet
        byte[] recvBuf = new byte[15000];
        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
        socket.receive(packet);

        //Packet received
        VisiCam.log(">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
        VisiCam.log(">>>Packet received; data: " + new String(packet.getData()));

        //See if the packet holds the right command (message)
        String message = new String(packet.getData()).trim();
        if (message.equals("VisiCamDiscover")) {
          byte[] sendData = responseUri.getBytes();

          //Send a response
          DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
          socket.send(sendPacket);

          VisiCam.log(">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
        }
      }
    } catch (IOException ex) {
      VisiCam.log("ERROR " + ex.getMessage());
    }
  }
}
