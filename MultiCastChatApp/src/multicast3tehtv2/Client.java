package multicast3tehtv2;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;


public class Client {
	public static boolean onkoAjastinPaalla = false;
	public static List<String> nimilista = new ArrayList<String>();
	public static InetAddress osoite;
	public static int portti = 42000;
	public static MulticastSocket mcSocket;
	public static void main(String[] args) {
		try {
		
		mcSocket = new MulticastSocket(portti); // Luodaan multicast-soketti
		osoite = InetAddress.getByName("239.0.0.1"); // M‰‰ritell‰‰n multicast IP-osoite	
		//apumuuttuja
		String poistu = "/leave";
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		//M‰‰ritet‰‰n l‰hetett‰vi‰ tietoja
		int versio = 2;
		int viesti = 1;	//1 = join, 2 = leave, 3 = viesti, 4 = update		
		int pv = 16;
		int month = 6;
		int year = 1997;
		
		String asiakasnimi = "TIEA322krmapale";			
		byte[] temp = asiakasnimi.getBytes("UTF-8");
		
		System.out.println("Syˆt‰ k‰ytt‰j‰nimi: ");
		String usernimi = System.console().readLine();
		byte[] userByte = usernimi.getBytes("UTF-8");
				
		byte[] tekstiByte = new byte[0];
		
		if(viesti == 1) {
			String teksti = "/join";
			tekstiByte = teksti.getBytes("UTF-8");
		}
		
		//asetetaan data-taulukon pituus
		byte[] data = new byte[7 + temp.length + userByte.length + tekstiByte.length]; 
		
		//Lis‰t‰‰n data-taulukkoon tarvittava informaatio oikeassa muodossa
		data = asetaTietoTavuihin(versio, viesti, pv, month, year, temp, userByte, tekstiByte, data.length);
		DatagramPacket dgramPacket = new DatagramPacket(data, data.length, osoite, portti);
		mcSocket.joinGroup(osoite); // Liityt‰‰n multicast ryhm‰‰n
		viesti = 3; // muutetaan viesti ilmaisemaan tekstin l‰hetystilaa
		dgramPacket.setData(data);
		mcSocket.send(dgramPacket);
		System.out.println("Tervetuloa MulticastChattiin! Poistuaksesi chatista, kirjoita: /leave");
		
		Timer ajastin = new Timer();
		TimerTask task = new TimerTask() {
			
			@Override
			public void run() {
				//TODO: l‰het‰ update viesti
				Client.UPDATE(versio, 4, pv, month, year, asiakasnimi, usernimi, nimilista);
				onkoAjastinPaalla = false;
				cancel();
			}
		};
		
		//T‰m‰ osio vastaa viestien l‰hett‰misest‰
		while(true) {
			try {		
				mcSocket.setSoTimeout(2500);
				//Viestin vastaanotto
				//t‰st‰ eteenp‰in pakettien vastaanottamiskokeilua
				byte[] rcv = new byte[256];
				DatagramPacket p = new DatagramPacket(rcv, rcv.length);
				mcSocket.receive(p);
				rcv = p.getData();
				int versiotemp = rcv[0] >> 4;
				int viestitemp = rcv[0] & 0x0f;
				int asiakasnimenPituus = rcv[4];
				int kayttajanimenPituus = rcv[ 5 + asiakasnimenPituus];
				if(versiotemp == 1 | versiotemp >= 2) {
					if(viestitemp >= 1 && viestitemp <= 3) {
						if(viestitemp == 1) {
					          nimilista.add(new String(rcv,(6 + asiakasnimenPituus), kayttajanimenPituus, StandardCharsets.UTF_8));
					          System.out.println("Lis‰tty nimi: " + nimilista.get(nimilista.size()-1));
					          if(nimilista.size() == 1) continue;
					          else {
					      		ajastin.schedule(task, 500);
					      		onkoAjastinPaalla = true;
					          }
						}
						if(viestitemp == 2) {
					          nimilista.remove(new String(rcv,(6 + asiakasnimenPituus), kayttajanimenPituus, StandardCharsets.UTF_8));
					          System.out.println("Poistettu nimi: " + new String(rcv,(6 + asiakasnimenPituus), kayttajanimenPituus, StandardCharsets.UTF_8));
						}
						//Jos viesti on 3, tulostetaan tiedot
						System.out.println(kayttajanimi(rcv) + ": " + new String(rcv, tekstinAlkuindex(rcv), tekstinpituus(rcv)));
					} 					
					if(viestitemp == 4) {
						if(onkoAjastinPaalla = true) {
							ajastin.cancel(); //joku muu ehti l‰hett‰‰ update viestin, suljetaan ajastin
						}
						if(nimilista.size() == 1) {
							nimilista = HaeNimet(rcv, usernimi);
							for(String nimi: nimilista) {
								System.out.println(nimi);
							}
						}
						if(nimilista.size() > 1) continue;
					}
					if(viestitemp > 4) rcv = new byte[256]; //resetataan tavutaulukko, koska paketti oli ep‰kelpoa muotoa
				}
				/*if(versio > 2) {
					if(viesti >= 1 && viesti <= 4) System.out.println(kayttajanimi(rcv) + ": " + new String(rcv, tekstinAlkuindex(rcv),
							tekstinpituus(rcv)));//System.out.println(new String(rcv, 0, p.getLength()))
					else rcv = new byte[256]; //Lis‰ttiin t‰m‰ osuus ylemp‰‰n ehtoon, n‰in v‰hemm‰n toistoa.
				}//t‰h‰n saakka kokeilua */ 
			}
			catch(SocketTimeoutException e) {
				//T‰m‰ j‰tet‰‰n huomiotta
			}
			catch(IllegalStateException e) {
				//J‰tet‰‰n k‰sittelem‰tt‰
			}
			catch(Exception e) {
				System.out.println("Merkint‰ 4 " + e);
			}
				
				//Viestin l‰hett‰mist‰				
				try {
					if(br.ready() == true) {
						//Thread.sleep(200);
						String kommentti = 	br.readLine();
						
						if(kommentti != null) {
							if(kommentti.equals(poistu)) {
								viesti = 2;
							}
							tekstiByte = kommentti.getBytes("UTF-8");
							data = new byte[7 + temp.length + userByte.length + tekstiByte.length];
							data = asetaTietoTavuihin(versio, viesti, pv, month, year, temp, userByte, tekstiByte, data.length);
							dgramPacket = new DatagramPacket(data, data.length, osoite, portti);
							dgramPacket.setData(data);
							mcSocket.send(dgramPacket);
							if(kommentti.equals(poistu)) {
								ajastin.cancel();
								break;	
							}
						}
					}
				} catch(NoSuchElementException e) {
					System.out.println("No such element");
					break;
				}	
		}

		mcSocket.leaveGroup(osoite);
		mcSocket.close();
	}
	
	catch(Exception e){
		System.out.println("Merkint‰ 3 " + e.getMessage());
	} 
}
	
	
	//l‰hetet‰‰n update viesti ajastimen loputtua
	public static void UPDATE(int versio, int viesti, int pv, int month, int year,
			String asiakasnimi, String usernimi, List<String> nimilista2) {
		try {	
		String[] nimiArray = new String[nimilista2.size()];
		nimiArray = nimilista2.toArray(nimiArray);
		byte[] data2 = asetaTavut(versio, viesti, pv, month, year, asiakasnimi, usernimi, nimiArray);
		DatagramPacket dgram = new DatagramPacket(data2, data2.length, osoite, portti);
		dgram.setData(data2);

			mcSocket.send(dgram);
		} catch (IOException e) {
			System.out.println("Merkint‰ 2 " + e);
		}
		
	}

	//palautetaan kayttajanimi
	private static String kayttajanimi(byte[] databyte) {
        int vakioPituus = 6;
        int asiakasnimenPituus = databyte[4];
        int kayttajanimenPituus = databyte[ 5 + asiakasnimenPituus];
        byte[] temp = new byte[kayttajanimenPituus];
        System.arraycopy(databyte, vakioPituus + asiakasnimenPituus, temp, 0, kayttajanimenPituus);
        String kayttaja = new String(temp, StandardCharsets.UTF_8);
		
		return kayttaja;
	}
	
	//palautetaan tulostamista varten indeksi, josta teksti alkaa
	private static int tekstinAlkuindex(byte[] databyte) {
        int vakioPituus = 7;
        int asiakasnimenPituus = databyte[4];
        int kayttajanimenPituus = databyte[ 5 + asiakasnimenPituus];
        int tekstinAlkuIndeksi = vakioPituus + asiakasnimenPituus + kayttajanimenPituus;		
		return tekstinAlkuIndeksi;
	}
	
	//palautetaan tulostamista varten tekstin pituus
	private static int tekstinpituus(byte[] databyte) {
        int vakioPituus = 6;
        int asiakasnimenPituus = databyte[4];
        int kayttajanimenPituus = databyte[ 5 + asiakasnimenPituus];
        int tekstinPituus = databyte[vakioPituus + asiakasnimenPituus + kayttajanimenPituus];
		
		return tekstinPituus;
	}
	
	//Hae nimet tavujonosta ja muuta stringeiksi. Luodaan nimilista ja palautetaan
	private static List<String> HaeNimet(byte[] rcv, String nimi) {
        List<String> nimetList = new ArrayList<String>();
        nimetList.add(nimi); //lis‰t‰‰n oma nimi listaan ensimm‰isen‰
        int vakioPituus = 6;
        int asiakasnimenPituus = rcv[4];
        int kayttajanimenPituus = rcv[ 5 + asiakasnimenPituus];
        int tekstinPituus = rcv[vakioPituus + asiakasnimenPituus + kayttajanimenPituus];//TODO: t‰‰ll‰ saattaa ilmet‰ virheit‰
        int tekstinAlkuIndeksi = vakioPituus + asiakasnimenPituus + kayttajanimenPituus;//TODO: --- pit‰isi olla nimet tekstin tilalla eik‰ j‰lkeen
        int nimiKentanIndeksi = tekstinAlkuIndeksi+1; //nimikentanindeksi taitaa olla indeksi jossa sijaitsee tieto nimen x pituudesta
            
        boolean b = true;
        int laskin = 0;
        while (b == true)
        {
            if (laskin >= tekstinPituus) b = false; //ehto silmukan loppumiselle
            else{	
                //nimikentanindeksi + 1 pit‰isi olla nimen alkukirjaimen indeksi
                nimetList.add(new String(rcv, nimiKentanIndeksi+1, rcv[nimiKentanIndeksi]));
                //lis‰t‰‰n laskimeen l‰pik‰ydyn nimen viem‰ tila
                laskin = laskin + rcv[nimiKentanIndeksi] + 1;
                //siirret‰‰n indeksi seuraavan nimen pituuden ilmaisevaan indeksiin
                nimiKentanIndeksi = nimiKentanIndeksi + rcv[nimiKentanIndeksi] + 1;
            }
        }
        if(nimetList.contains(nimi)) {
        	if(nimetList.lastIndexOf(nimi) != 0) nimetList.remove(nimi); //poistetaan duplikaatti omasta k‰ytt‰j‰nimest‰
        	//n‰in saadaan oma nimi ensimm‰iseksi listassa ja duplikaatti poistuu
        	//Ongelmia tulee, jos kahdella on sama k‰ytt‰j‰nimi, mutta ei t‰m‰n teht‰v‰n toteutuksen kannalta oleellista
        }
        return nimetList;
	}


	

		///Asetetaan l‰hetett‰v‰ tieto oikeaan muotoon. Viestit 1-3.
		public static byte[] asetaTietoTavuihin(int versio, int viesti, int pv, int month, int year,
				byte[] temp, byte[] userByte, byte[] tekstiByte, int datalength) {
			
			byte[] data = new byte[datalength];
			
			data[0] = (byte)(versio << 4);
			data[0] = (byte)(data[0] ^ viesti);
			data[1] = (byte)(pv << 3);
	        data[1] = (byte)((month >> 1) ^ data[1]);
	        data[2] = (byte)(month << 7);
	        data[2] = (byte)(data[2] ^ (year >> 4));
	        data[3] = (byte)(year << 4);
	        data[4] = (byte)(temp.length); //tallennetaan tieto asiakasnimen viem‰st‰ tilan m‰‰r‰st‰

	        int indeksi = 5;
	        //asetetaan asiakasnimi tavuihin
	        for (int i = 0; i < temp.length; i++)
	        {
	            data[indeksi] = temp[i];
	            indeksi++;
	        }

	        //tallennetaan tieto usernimeen varattavasta tilan m‰‰r‰st‰
	        data[indeksi++] = (byte)(userByte.length);

	        //asetetaan usernimi tavuihin
	        for (int i = 0; i < userByte.length; i++)
	        {
	            data[indeksi] = userByte[i];
	            indeksi++;
	        }
	        
		        //tallennetaan tieto tekstin viem‰n tilan m‰‰r‰st‰
		        data[indeksi++] = (byte)(tekstiByte.length);

		        //asetetaan teksti tavuihin
		        for(int i = 0; i < tekstiByte.length; i++)
		        {
		            data[indeksi] = tekstiByte[i];
		            indeksi++;
		        }
			
			return data;
		}
		
		//Muutetaan tarvittavat tiedot tavuiksi. Viesti 4, update.
	    public static byte[] asetaTavut(int versio, int viesti, int day, int month, int year, String asiakasnimi,
                String usernimi, String[] nimet){
	    	    	
	    		// selvit‰ string-kenttien pituudet UTF-8 tavuina
		    	int clientLength = asiakasnimi.length(); // UTF-8 koodattujen tavujen m‰‰r‰
		    	int userLength = usernimi.length(); // UTF-8 koodattujen tavujen m‰‰r‰
		    	// dataLength pit‰‰ olla kaikkien nimien yhteispituus + nimien lukum‰‰r‰
		    	// Nimet UTF-8 koodattuina, joita ennen yhden tavun kentt‰, joka
		    	// m‰‰ritt‰‰ nimen vaatimien tavujen lukum‰‰r‰n
		    	int dataLength = 0;
		    	for (String nimi: nimet)
		    	{
		    	dataLength = dataLength + nimi.length() + 1;
		    	}
		    	// Otsikon vakiopituiset kent‰t on 7 tavua
		    	int constLength = 7;
		    	byte[] tavut = new byte[constLength + clientLength + userLength + dataLength];  
		    	
		    	try {
		    		byte[] temp = asiakasnimi.getBytes("UTF-8");
			    	byte[] userByte = usernimi.getBytes("UTF-8");
			    	byte[] dataByte = new byte[dataLength]; //nimilistan pituus

			    	tavut[0] = (byte)(versio << 4); //versio = (tavut[0] >> 4) & 0x0f;
			    	tavut[0] = (byte)(tavut[0] ^ viesti);     //viesti = tavut[0] & 0x0f;

			    	tavut[1] = (byte)(day << 3);
			    	tavut[1] = (byte)((month >> 1) ^ tavut[1]);
			    	tavut[2] = (byte)(month << 7);
			    	tavut[2] = (byte)(tavut[2] ^ (year >> 4));
			    	tavut[3] = (byte)(year << 4);
			    	tavut[4] = (byte)(temp.length); //tallennetaan tieto asiakasnimen viem‰st‰ tilan m‰‰r‰st‰

			    	int indeksi = 5;
			    	//asetetaan asiakasnimi tavuihin
			    	for (int i = 0; i < temp.length; i++)
			    	{
			    	tavut[indeksi] = temp[i];
			    	indeksi++;
			    	}

			    	//tallennetaan tieto usernimeen varattavasta tilan m‰‰r‰st‰
			    	tavut[indeksi++] = (byte)(userByte.length);

			    	//asetetaan usernimi tavuihin
			    	for (int i = 0; i < userByte.length; i++)
			    	{
			    	tavut[indeksi] = userByte[i];
			    	indeksi++;
			    	}

			    	//asetetaan tieto nimien viem‰st‰ tilan m‰‰r‰st‰
			    	tavut[indeksi++] = (byte)(dataLength);

			    	//asetetaan tieto nimist‰ ja niiden viemist‰ tiloista tavuihin
			    	for(int i = 0; i < nimet.length; i++) //for(int i = 0; i < nimet.GetLength(0); i++)
			    	{
			    	tavut[indeksi] = (byte)(nimet[i].length()); // asetetaan nimen pituus tavuihin
			    	int apu = nimet[i].length(); //s‰ilytet‰‰n nimen pituus tulevaa silmukkaa varten

			    	dataByte = nimet[i].getBytes("UTF-8");
			    	indeksi++;
			    		for (int y = 0; y < apu; y++)
			    		{
			    			tavut[indeksi] = dataByte[y];
			    			indeksi++;
			    		}
			    	}
	    	}
	    	catch(Exception e){
	    		System.out.println("Merkint‰ 1 " + e);
	    	}
		    	
		    	return tavut;
	    }
		

}

