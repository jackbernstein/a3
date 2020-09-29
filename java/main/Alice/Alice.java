import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Alice {

	// Constants for RSA keys
	private static String PUBLIC_KEY_PATH = "alicePublic.pub";
	private static String PRIVATE_KEY_PATH = "alicePrivate.key";
	private static String PUBLIC_KEY_FORMAT = "X.509";
	private static String PRIVATE_KEY_FORMAT = "PKCS#8";

	//RSA keys 
	private PrivateKey privateKey;
	private PublicKey publicKey;

    // instance variables
    private boolean mac;
    private boolean enc;

    public Base64.Encoder encoder = Base64.getEncoder();
    public Base64.Decoder decoder = Base64.getDecoder();
    
    public Alice(String malloryPort, String config) throws Exception {

		//Apply configuration
		if(config.compareTo("noCrypto") == 0){
			mac = false;
			enc = false;
		} else if(config.compareTo("enc") == 0){
			mac = false;
			enc = true;
		} else if(config.compareTo("mac") == 0){
			mac = true;
			enc = false;
		} else if(config.compareTo("EncThenMac") == 0){
			mac = true;
			enc = true;
		}

		// Read in RSA keys 
		readKeys();

		Scanner console = new Scanner(System.in);
		System.out.println("This is Alice"); 
			
		//obtain server's port number and connect to it
		int serverPort = Integer.parseInt(malloryPort);
		String serverAddress = "localhost";

		try{
			Socket serverSocket = new Socket(serverAddress, serverPort);
			System.out.println("Connected to Server Mallory");
			DataOutputStream streamOut = new DataOutputStream(serverSocket.getOutputStream());
				
			//obtain the message from the user and send it to Server
			//the communication ends when the user inputs "done"
			String line = "";
			int counter = 0;
			while(!line.equals("done")) {
				try {  
					counter++;
					System.out.print("Type message: ");
					line = console.nextLine();
					
					String packagedMsg = packageMessage(line + ":" + counter);
					streamOut.writeUTF(packagedMsg);
					streamOut.flush();
					System.out.println("Message en route to Bob");
				} 
				catch(IOException ioe) {  
					System.out.println("Sending error: " + ioe.getMessage());
				}
			}
			//close all the sockets and console 
			console.close();
			streamOut.close();
			serverSocket.close();
		} 
		catch(IOException e) {
				//print error
				System.out.println("Connection failed due to following reason");
				System.out.println(e);
		}
	}	
		
	private void readKeys() {
		try {
			/* Read all bytes from the private key file */
			Path path = Paths.get(PRIVATE_KEY_PATH);
			byte[] bytes = Files.readAllBytes(path);

			/* Generate private key. */
			PKCS8EncodedKeySpec ks1 = new PKCS8EncodedKeySpec(bytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			privateKey = kf.generatePrivate(ks1);

			/* Read all the public key bytes */
			path = Paths.get(PUBLIC_KEY_PATH);
			bytes = Files.readAllBytes(path);

			/* Generate public key. */
			X509EncodedKeySpec ks2 = new X509EncodedKeySpec(bytes);
			kf = KeyFactory.getInstance("RSA");
			publicKey = kf.generatePublic(ks2);
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		} 
		catch (NoSuchAlgorithmException e) {
			System.out.println(e.getMessage());
		} 
		catch (InvalidKeySpecException e) {
			System.out.println(e.getMessage());
		} 
	}

	private String packageMessage(String message) throws Exception {
		StringBuilder acc = new StringBuilder();
		acc.append(message);
		
		return acc.toString();
    }
    
    /**
     * args[0] ; port that Alice will connect to (Mallory's port)
     * args[1] ; program configuration
     */
    public static void main(String[] args) {
	
		//check for correct # of parameters
		if (args.length != 2) {
			System.out.println("Incorrect number of parameters");
		} else {
			//Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			//create Alice to start communication
			try {
				Alice alice = new Alice(args[0], args[1]);
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	
    }
}
