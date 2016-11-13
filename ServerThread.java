import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ServerThread extends Thread {
    private ServerSocket mSocket;
    private int MAX_SIZE = 0;
    private int numBytes = 0;
    private byte HEADER = 0b01111110;
    private static int PAYLOAD_SIZE = 128;
    private static final String FILE_NAME = "umdlogo.jpg";
    private static final String DESKTOP = "D:/Workspace/Java/Server/bin/";
    private static final String LAPTOP = "C:/Users/yitzchak/Workspace/Server/bin/";
    private static String ROOT_PATH = LAPTOP;
    private static final String FILE_LARGE = "bugsbunny1.wav";
    private static final String VERY_LARGE = "09 Rap God.mp3";
    Scanner scanner = new Scanner(System.in);
    private ArrayList<Byte[]> contents = new ArrayList<Byte[]>();
    private static final boolean largeFile = true;
    private static final boolean veryLargeFile = true;

    public ServerThread(int portNum) throws IOException {
        mSocket = new ServerSocket(portNum);
        // Wait infinitely or until server killed
        mSocket.setSoTimeout(0);
    }
    
    public void sendACK(DataOutputStream out, Socket server, int count) throws IOException {
        byte[] response = (!largeFile) ? new byte[3] : new byte[4];
        
        if (!largeFile) {
            response[2] = HEADER;
            response[1] = (byte) (0xFF & count);
            response[0] = 0x01;
        } else {
            response[3] = HEADER;
            response[2] = (byte) ((0xFF00 & count) >> 8);
            response[1] = (byte) (0xFF & count);
            response[0] = 0x01;
        }
        
        out.write(response);
    }
    
    public void sendNACK(DataOutputStream out, Socket server, int count) throws IOException {
        byte[] response = (!largeFile) ? new byte[3] : new byte[4];
        
        if (!largeFile) {
            response[2] = HEADER;
            response[1] = (byte) (0xFF & count);
            response[0] = 0x00;
        } else {
            response[3] = HEADER;
            response[2] = (byte) ((0xFF00 & count) >> 8);
            response[1] = (byte) (0xFF & count);
            response[0] = 0x00;
        }
        
        //System.out.println("Sending NACK..." + count);
        
        out.write(response);
    }
    
    private void getMetaData() {
        Socket meta = null;
        
        System.out.println("Waiting for client on port: "
                + mSocket.getLocalPort());
        
        // Client says it will send and sends size of file
        try {
            meta = mSocket.accept();
            
            System.out.println("Connected to " + meta.getRemoteSocketAddress());
            DataInputStream input = new DataInputStream(meta.getInputStream());
            
            byte[] metaData = (!largeFile) ? new byte[1 + 1 + 2] : (!veryLargeFile) ? new byte[1 + 2 + 2] : new byte[1 + 2 + 3];
            
            if (!largeFile) {
                input.read(metaData, 0, 1 + 1 + 2);
                MAX_SIZE = 0xFF & metaData[2];
                
                // Send max number in ACK
                sendACK(new DataOutputStream(meta.getOutputStream()), meta, 0xFF);
                numBytes = ((0xFF & metaData[1]) << 8) + (0xFF & metaData[0]);
            } else {
                if (!veryLargeFile) {
                    input.read(metaData, 0, 1 + 2 + 2);
                    MAX_SIZE = ((0xFF & metaData[3]) << 8) + (0xFF & metaData[2]);
                
                    numBytes = ((0xFF & metaData[1]) << 8) + (0xFF & metaData[0]);
                } else {
                    input.read(metaData, 0, 1 + 2 + 3);
                    
                    //System.out.println("[4]: " + metaData[4] + " [3]: " + metaData[3]);
                    
                    MAX_SIZE = ((0xFF & metaData[4]) << 8) + (0xFF & metaData[3]);
                
                    numBytes = ((0xFF & metaData[2]) << 16) + ((0xFF & metaData[1]) << 8) + (0xFF & metaData[0]);
                }
                
                // Send max number in ACK
                sendACK(new DataOutputStream(meta.getOutputStream()), meta, 0xFFFF);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Socket has timed out");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (meta != null) {
                    meta.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        System.out.println("PACKETS: " + MAX_SIZE + " NUM_BYTES: " + numBytes);
    }
    
    private void saveFile() {
        File saveTo = (!largeFile) ? new File(FILE_NAME) : (!veryLargeFile) ? new File(FILE_LARGE) : new File(VERY_LARGE);
        FileOutputStream fos = null;
        int currSize = 0;
        
        System.out.println("Saving to file: " + saveTo);
        
        try {
            fos = new FileOutputStream(saveTo);
            //System.out.println("Array Size: " + contents.size());
            for (int i = 0; i < contents.size(); i++) {
                Byte[] tempB = contents.get(i);
                byte[] temp = new byte[tempB.length];
                
                for (int j = 0; j < temp.length; j++) {
                    //System.out.println("j: " + j);
                    currSize++;
                    temp[j] = tempB[j];
                }
                
                //System.out.println("[127] " + temp[127] + " [126] " + temp[126] + " [125] " + temp[125]);
                // When size is more that the number of bytes expected
                // remove the excess bytes when writing to file
                if (currSize > numBytes) {
                    //System.out.println("currSize: " + currSize + " numBytes: " + numBytes + " temp.length: " + temp.length);
                    fos.write(temp, 0, temp.length - (currSize - numBytes));
                    break;
                } else {
                    fos.write(temp, 0, temp.length);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    private void getFile(ShowPNG frame) {
        int current = 0;
        Byte[] payload;
        Socket server = null;
        DataInputStream dis = null;
        DataOutputStream out = null;
        
        // Set timeout to 1 second
        try {
            mSocket.setSoTimeout(1000);
        } catch (SocketException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        try {
            server = mSocket.accept();
            System.out.println("Retrieving file contents");
            dis = new DataInputStream(server.getInputStream());
            out = new DataOutputStream(server.getOutputStream());
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
            return;
        }
        
        // Get the file to be sent
        for (int i = 0; i < MAX_SIZE; i++) {
            try {
                // H    C   127 126 ... 0   CRC
                // 130  129 128 127 ... 1   0
                byte[] packet = (!largeFile) ? new byte[PAYLOAD_SIZE + 3] : new byte[PAYLOAD_SIZE + 4];
                
                if (server.isClosed()) {
                    break;
                }
                
                if (!largeFile) {
                    dis.read(packet, 0, PAYLOAD_SIZE + 3);
                } else {
                    dis.read(packet, 0, PAYLOAD_SIZE + 4);
                }
                //System.out.println("[128] " + packet[128] + " [127] " + packet[127] + " [126] " + packet[126]);
                current = (!largeFile)
                        ? (0xFF & packet[packet.length - 2])
                        : (0xFF & (packet[packet.length - 2]) << 8) + (0xFF & packet[packet.length - 3]);
                
                CRC16 crc = new CRC16(0x1D, packet);
                
                if (crc.checkFrame() && packet[packet.length - 1] == HEADER) {
                    int size = (!largeFile) ? packet.length - 3 : packet.length - 4;
                    payload = new Byte[size];
                    
                    // 127 126 ... 0
                    for (int j = 0; j < size; j++) {
                        payload[j] = packet[j + 1];
                    }
                    
                    //System.out.println("[127] " + payload[127] + " [126] " + payload[126] + " [125] " + payload[125]);
                    //System.out.print("Current Pos: " + i);
                    // Packets received:
                    // last ... 2nd 1st
                    // (l - 1)...1   0
                    contents.add(payload);
                    
                    //System.out.println("Contents: " + contents);
                    
                    sendACK(out, server, current);
                    
                    if (current % 10000 == 0 && current != MAX_SIZE - 1) {
                        int percentage = (current / MAX_SIZE) * 100;
                        frame.setTitle("Downloading..." + percentage + "%");
                        frame.repaint();
                        //System.out.println("Updating title");
                    } else if (current == MAX_SIZE - 1) {
                        frame.setTitle("Downloaded...100%");
                        frame.repaint();
                    }
                    
                } else {
                    i = current;
                    System.out.println("CRC fails, request retry sending: " + i);
                    sendNACK(out, server, current);
                }
            } catch (SocketTimeoutException | SocketException e) {
                System.out.println("Socket has timed out, request retry sending: " + current);
                try {
                    sendNACK(out, server, current);
                    i = current;
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //scanner.nextLine();
        }
        
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public void run() {
        // Display loading bar
        ShowPNG temp = new ShowPNG(ROOT_PATH + ((!largeFile) ? FILE_NAME : FILE_LARGE));
        temp.setVisible(true);
        temp.setSize(new Dimension(500, 500));
        
        // Wait until client requests
        // Get size of file and number of frames to be received
        getMetaData();
        
        // Get the file
        // Save to a file
        getFile(temp);
        
        saveFile();
        
        temp.setTitle("Saved!");
        
        // Display the file
        if (!largeFile) {
            JPanel panel = new JPanel();
            panel.setBackground(Color.BLACK);
            ImageIcon icon = new ImageIcon(ROOT_PATH + ServerThread.FILE_NAME);
            
            JLabel label = new JLabel();
            label.setIcon(icon);
            panel.add(label, BorderLayout.CENTER);
            //panel.setSize(new Dimension(500, 500));
            panel.setPreferredSize(new Dimension(500, 500));
            temp.getContentPane().add(panel);
            temp.revalidate();
        }
    }
    
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        Thread t = null;
        
        if (largeFile && veryLargeFile) PAYLOAD_SIZE = 1024;
        
        try {
           t = new ServerThread(port);
           t.start();
        } catch(IOException e) {
           e.printStackTrace();
        }
    }
}
