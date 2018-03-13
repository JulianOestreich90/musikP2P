
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * 
 * Peer zum Abgleich von Musikdaten
 * Erzeugt Socket für Verbindung mit anderen Peers auf den 
 * Ports 50002, 50005 und 50007.
 * 
 * @author Julian Oestreich   
 */

public class Client {
  
  /**
   * Zulässige Portnummern.
   */
  private static final int[] ports=new int[]{ 50002,
                                                50005,
                                                50007};
  
  /**
   * Initiale Datensätze als statisches String[].
   */
  private static final String[] initKeys=new String[]{"Interpret: Beatles Titel: I Wanna Be Your Man",
                                                        "Interpret: Sportfreunde Stiller Titel: Ein Kompliment",
                                                        "Interpret: Beatles Titel: All My Loving",
                                                        "Interpret: Rolling Stones Titel: Satisfaction",
                                                        "Interpret: Michael Jackson Titel: Thriller",
                                                        "Interpret: Razorlight Titel: Wire to Wire"};
  
  /**
   * statische ArrayList vom Typ String, die den initialen Datensatz + empfangene Datensätze enthält.
   */
  public static ArrayList<String> data =  new ArrayList<String>();
  
  /**
   * Startet das Programm
   * @param args Portnummer 
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {    
        
        // Bei zulässigen args-Eingaben Programm starten. Unzulässige Übergaben von args abfangen.
        
        if(args.length!=1){
            System.out.println("Bitte genau eine Portnummer auf der Daten empfangen werden sollen angeben ");
        } else{
            int prt;
            try{
                prt = Integer.parseInt(args[0]);
                if(prt==50002||prt==50005||prt==50007){
                
                //System.out.println(prt);
                startSender(prt);
                startReceiver(prt);
                
                /**
                 * Benutzerinput fordern
                 * "show" um Datensatz des Peers zu loggen
                 * "exit" um Peer zu beenden.
                 */
                BufferedReader keyboard=new BufferedReader(new InputStreamReader(System.in));
                String command;
                System.out.println("Musik P2P gestartet. Um Daten des Peers anzuzeigen tippe bitte 'SHOW'"
                        + ", um das Programm zu beenden 'EXIT'");
                while(true){
                    System.out.println("Eingabe machen:");
                    command = keyboard.readLine();
                    if(command.equalsIgnoreCase("exit")) {
                        System.exit(0);
                    }
                    if(command.equalsIgnoreCase("show")) {
                        System.out.println();
                        System.out.println("Aktuelle Daten on Port " + prt + ":\n");
                        for(int i = 0; i < data.size(); i++) {
                            System.out.println(data.get(i));
                        }
                        System.out.println("");
                        
                    } else{
                        System.out.println("Bitte machen Sie eine gültige Eingabe, ohne '' ");
                    }
                }
            } else{
                System.out.println("Bitte nur Port 50002, 50005 oder 50007 verwenden.");
            }
            } catch(NumberFormatException e) {
                System.out.println("Bitte nur Ziffern als Argument übergeben!");
            }          
        }        
  }
  
  
/**
 * Methode um UDP-Pakete zu Versenden. 
 * Läuft in eigenem Thread
 * @param arg Portnummer
 * @throws UnknownHostException 
 */ 

  public static void startSender(int arg) throws UnknownHostException{          
      
    InetAddress aHost = InetAddress.getLocalHost();       
                    
    (new Thread() {
        @Override
        public void run() {
            /**
             * keys speichert für den gewählten der 3 zulässigen Ports zwei verschiedene initiale Datensätze.
             */
            DatagramSocket socket = null;
            CopyOnWriteArraySet<String> keys=new CopyOnWriteArraySet<String>();
            for(int i=0;i<3;i++)
                if(arg==ports[i]){
                    keys.add(initKeys[2*i]);               
                    keys.add(initKeys[2*i+1]);
                    //System.out.println("Host on Port " + arg + " contains the following Data: \n " + initKeys[2*i] + "\n" + initKeys[2*i+1]);
                }
            
            /**
             * UDP-Socket für P2P-Verbindung.
             */
            try {
                socket = new DatagramSocket();
                
                //socket.setBroadcast(true);
            } catch (SocketException ex) {
                ex.printStackTrace();
                //parent.quit();
            }
            /**
             * Sende ein UDP-Paket für jeden key in keys und an jeden port in ports, außer an den eigenen.
             */
            while(true) {
                for(String key:keys){
                    if(!data.contains(key)) {
                        data.add(key);
                    }                   
                    for(int port:ports){
                        if(port!=arg) {
                            try{
                        byte[] buf = key.getBytes();
                        DatagramPacket pack = new DatagramPacket(
                                buf,
                                buf.length,
                                aHost,
                                port);
                        socket.send(pack);
                        //System.out.println("Client is sending this: " + new String(pack.getData()) + " to Port: " + pack.getPort());
                        //Thread.sleep(1000);
                    } catch (IOException ex) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        }                             
                    }                
                }
            }
        }}}).start();
    }
  

/**
 * Methode um UDP-Pakete zu empfangen.
 * Läuft in eigenem Thread
 * @param port Portnummer
 */
  public static void startReceiver(int port)  {
    (new Thread() {
        @Override
        public void run() {

                //byte data[] = new byte[0];
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket(port);
                    //socket.setBroadcast(true);;

                } catch (SocketException ex) {
                    ex.printStackTrace();
                    //parent.quit();
                }
                DatagramPacket packet = new DatagramPacket(new byte[256], 256);
                String temp;
                
                while (true) {
                try {
                    socket.receive(packet);
                    temp=new String(packet.getData(), 0, packet.getLength());
                    
                    if(!data.contains(temp)){
                        data.add(temp);
                        //System.out.println("\n"+ temp +" was received on "+ socket.getLocalPort() + " from " + packet.getSocketAddress() );
                    }
                    /*
                    System.out.println("\nThis is the Data the Client on Port "+ socket.getLocalPort() +" has received from the Network: ");
                    for(int i = 0; i < data.size(); i++) {
                        System.out.println(data.get(i));
                    }
                    */
                    //System.out.println("Message received ..."+ temp);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    //parent.quit();
                    }
                }
            }
    }).start();
  }
}
    
    

x
