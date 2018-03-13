import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Peer zum Abgleichen von Musikdaten.
 * erzeugt Socket und Protokoll für Verbindung mit anderen Peers auf den Ports 50003, 50007 und 50008.
 */
public final class MusikPeer
{
    /**
     * Zulässige Ports für P2P-Verbindung.
     */
    private static final int[] ports=new int[]{ 50003,
                                                50007,
                                                50008};
    /**
     * Initiale Schlüsselwerte.
     */
    private static final String[] initKeys=new String[]{"Interpret: Beatles Titel: I Wanna Be Your Man",
                                                        "Interpret: Sportfreunde Stiller Titel: Ein Kompliment",
                                                        "Interpret: Beatles Titel: All My Loving",
                                                        "Interpret: Rolling Stones Titel: Satisfaction",
                                                        "Interpret: Michael Jackson Titel: Thriller",
                                                        "Interpret: Razorlight Titel: Wire to Wire"};
    /**
     * Gibt Fehlermeldungen aus.
     * @param msg Fehlermeldung
     * @param e dazugehörige Exception
     */
    private static void printError(String msg, Exception e)
    {
        System.err.println(msg);
        if(e!=null) System.err.println(e);
    }
    /**
     * Programmstart.
     */
    public static void main(String[] args)
    {
        System.out.println("MusikPeer wird gestartet.");
        DatagramSocket sock=null;
        for(int port:ports)
            try
            {
                sock=new DatagramSocket(port);
                break;
            }
            catch(SocketException e){}
        if(sock==null)
        {
            printError("Fehler: Kein freier Port verfügbar!",null);
            System.exit(-1);
        }
        CopyOnWriteArraySet<String> keys=new CopyOnWriteArraySet<String>();
        for(int i=0;i<3;i++)
            if(sock.getLocalPort()==ports[i])
            {
                keys.add(initKeys[2*i]);
                keys.add(initKeys[2*i+1]);
            }
        new MusikPeer(sock,keys).action();  // Protokolle und Dialog starten
    }
    /**
     * UDP-Socket für P2P-Verbindung.
     */
    private DatagramSocket sock;
    /**
     * Protokollobject für Senden.
     */
    private Thread sendproll;
    /**
     * Protokollobject für Empfangen.
     */
    private Thread recvproll;
    /**
     * Sammlung eigener Schlüssel.
     */
    private Collection<String> keys;
    /**
     * Public-Konstruktor,
     * baut Sender- und Empfängerprotokolle auf,
     * keine null-Objekte erlaubt.
     * @param s DatagramSocket für Verbindung
     * @param k Sammlung eigener Schlüssel
     */
    public MusikPeer(DatagramSocket s,Collection<String> k)
    {
        keys=k;
        sock=s;
        sendproll=null;
        recvproll=null;
        try
        {
            sendproll=new Thread(new MusikSendProtocoll(sock,keys));
            recvproll=new Thread(new MusikReceiveProtocoll(sock,keys));
            sendproll.start();
            recvproll.start();
        }
        catch(Exception e)
        {
            printError("Fehler: Transfer-Protokolle konnten nicht gestartet werden!",e);
            release(true);
        }
    }
    /**
     * Startet Dialog zur Eingabe.
     */
    public void action()
    {
        BufferedReader keyboard=new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Dialog gestartet. Schlüssel ausgeben mit SHOW. Beenden mit EXIT.");
        try
        {
            String command;
            while(true)
            {
                command=keyboard.readLine(); // Eingabe lesen
                if(command.equalsIgnoreCase("exit"))  // beenden
                {
                    release(false);
                    return;
                }
                else if(command.equalsIgnoreCase("show"))
                {
                    System.out.println();
                    System.out.println("Aktuelle Schlüssel:");
                    for(String key:keys)
                        System.out.println(key);
                }
                else
                {
                    printError("Fehler: Unbekannter Befehl: "+command+"!",null);
                }
            }
        }
        catch(Exception e)
        {
            printError("Fehler: Verbindung unterwartet unterbrochen!",null);
            release(true);
        }
    }
    /**
     * Beendet das Programm.
     * @param error mit Error beenden genau dann wenn true.
     */
    private void release(boolean error)
    {
        System.out.println("Verbindung wird getrennt.");
        if(sendproll!=null) // Protokoll beenden
        {
            Thread tmp=sendproll;
            sendproll=null;
            tmp.interrupt();
        }
        if(recvproll!=null) // Protokoll beenden
        {
            Thread tmp=recvproll;
            recvproll=null;
            tmp.interrupt();
        }
        try // Socket schließen
        {
            if(sock!=null) sock.close();
            System.out.println("Dialog wurde beendet.");
            if(error) System.exit(-1);
            else System.exit(0);
        }
        catch(Exception e)
        {
            printError("Fehler: Fehler beim Trennen der Verbindung!",e);
            System.exit(-1);
        }
    }
    /**
     * Senderprotkollklasse.
     */
    private final class MusikSendProtocoll implements Runnable
    {
        /**
         * UDP-Socket für P2P-Verbindung.
         */
        private DatagramSocket sock;
        /**
         * Sammlung eigener Schlüssel,
         */
        private Collection<String> keys;
        /**
         * Konstruktor initialisiert Senderprotkoll.
         */
        public MusikSendProtocoll(DatagramSocket s,Collection<String> k)
        {
            sock=s;
            keys=k;
        }
        /**
         * Sendet Schlüssel zu in MusikPeer gegebenen Ports.
         */
        public void run()
        {
            while(true)
                for(String key:keys)
                    for(int port:MusikPeer.ports)
                        if(sock.getLocalPort()!=port)
                            try
                            {
                                byte[] buf=key.getBytes();
                                DatagramPacket pack=new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(), port);
                                sock.send(pack);
                            }
                            catch(Exception e)
                            {
                                if(!sock.isClosed()) MusikPeer.printError("Fehler: Senden von "+key+" an "+port+" fehlgeschlagen!",e);
                            }
        }
    }
    /**
     * Empfängerprotkollklasse.
     */
    private final class MusikReceiveProtocoll implements Runnable
    {
        /**
         * UDP-Socket für P2P-Verbindung.
         */
        private DatagramSocket sock;
        /**
         * Sammlung eigener Schlüssel,
         */
        private Collection<String> keys;
        /**
         * Konstruktor initialisiert Empfängerprotkoll.
         */
        public MusikReceiveProtocoll(DatagramSocket s,Collection<String> k)
        {
            sock=s;
            keys=k;
        }
        /**
         * Empfängt Schlüssel von in MusikPeer gegebenen Ports.
         */
        public void run()
        {
            while(true)
                try
                {
                    byte[] buf=new byte[256];
                    DatagramPacket pack=new DatagramPacket(buf, buf.length);   // empfangen, falls keine eigenen Schlüssel
                    sock.receive(pack);
                    if(pack.getAddress().getHostAddress().equals(InetAddress.getLocalHost().getHostAddress()))
                        for(int port:MusikPeer.ports)
                            if(pack.getPort()==port)
                            {
                                String nullTerminatedKey="";
                                String notNullTerminatedKey=new String(buf,0,buf.length,"utf-8");
                                if(notNullTerminatedKey!=null)
                                    for(char c:notNullTerminatedKey.toCharArray())
                                    {
                                        if(c=='\u0000') break;
                                        nullTerminatedKey+=c;
                                    }
                                if(nullTerminatedKey!="") keys.add(nullTerminatedKey);
                                break;
                            }
                }
                catch(Exception e)
                {
                    if(!sock.isClosed()) MusikPeer.printError("Fehler: konnte DatagramPacket nicht empfangen!",e);
                }
        }
    }
}
