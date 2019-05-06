import config.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {
    private EnhancedDatagramSocket udp;
    private String data;
    private int sequenceNumber;
    private int acknowledgmentNumber;
    private enum handShakeStates {CLOSED , SYN_SENDING ,SYN_SENT , SENDING_ACK , ESTAB};
    private handShakeStates handShakeState;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.udp = new EnhancedDatagramSocket(port);
        this.sequenceNumber = (new Random().nextInt( Integer.MAX_VALUE ) + 1)%10000;
        this.handShakeState = handShakeStates.CLOSED;
        this.udp.setSoTimeout(Config.TIMEOUT);
    }

    @Override
    public void send(String pathToFile, String destinationIp , int destinationPort) throws Exception {
        this.data = readDataFromFile(pathToFile);
        this.handShake(destinationIp , destinationPort);
    }

    private String readDataFromFile(String pathToFile) throws IOException {
        String data;
        data = new String(Files.readAllBytes(Paths.get(pathToFile)));
        return data;
    }

    private void changeStateToListen(){
        this.handShakeState = handShakeStates.SYN_SENDING;
    }

    private void sendingSyn(String destinationIp, int destinationPort) {
        while(true) {
            try {
                TCPPacket sendPacket = new TCPPacket(destinationIp, destinationPort, sequenceNumber, 0, false, true, "");
                this.udp.send(sendPacket.getUDPPacket());
                this.handShakeState = handShakeStates.SYN_SENT;
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void synSent() {
        while(true) {
            try {
                byte[] buff = new byte[1408];
                DatagramPacket data = new DatagramPacket(buff, buff.length);
                this.udp.receive(data);
                TCPPacket receivedPacket = new TCPPacket(data);
                if (receivedPacket.getAckFlag() && receivedPacket.getSynFlag()) {
                    this.acknowledgmentNumber = receivedPacket.getSquenceNumber();
                    this.sequenceNumber++;
                    this.handShakeState = handShakeStates.SENDING_ACK;
                    break;
                }
            }catch (SocketTimeoutException e){
                this.handShakeState = handShakeStates.SYN_SENDING;
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendingAck(String destinationIp, int destinationPort) {
        while (true) {
            try {
                //TODO: send multiple ack packet
                TCPPacket sendPacket = new TCPPacket(destinationIp, destinationPort, sequenceNumber, acknowledgmentNumber + 1, true, false, "");
                this.udp.send(sendPacket.getUDPPacket());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void establishing() {
        this.handShakeState = handShakeStates.ESTAB;
    }

    private boolean handShake(String destinationIp , int destinationPort) throws Exception {
        while(true){
            switch (this.handShakeState){
                case CLOSED:
                    changeStateToListen();
                case SYN_SENDING:
                    sendingSyn(destinationIp , destinationPort);
                case SYN_SENT:
                    synSent();
                case SENDING_ACK:
                    sendingAck(destinationIp , destinationPort);
                case ESTAB:
                    establishing();
            }
        }
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }
}
