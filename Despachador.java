/*
 * Servicio para las peticiones y respuestas de cara al exterior.
 * */



package principal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Semaphore;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Singleton
@Path("/despachador")
public class Despachador
{
	Proceso p1, p2;
	long t1, t2;
	public Semaphore sem = new Semaphore(0);

	
	/**Metodo que se llamara para crear los procesos en cada maquina.
	 * 
	 * @param server Variable para indentificar cada servidor.
	 * @param ip1 Ip del primer servidor.
	 * @param ip2 Ip del segundo servidor.
	 * @param ip3 Ip del tercer servidor.
	 * @return
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("crearProcesos")
	public void crearProcesos(@QueryParam("servidor") char server, @QueryParam("ip1") String ip1,
			@QueryParam("ip2") String ip2, @QueryParam("ip3") String ip3) {
		
		switch(server){
		
			case 'A':
				p1= new Proceso(1,ip1,ip2,ip3);
				p2= new Proceso(2,ip1,ip2,ip3);
				p1.setName("Proceso 1");
				p2.setName("Proceso 2");
				break;
			
			case 'B':
				p1= new Proceso(3,ip1,ip2,ip3);
				p2= new Proceso(4,ip1,ip2,ip3);
				p1.setName("Proceso 3");
				p2.setName("Proceso 4");
				break;
				
			case 'C':
				p1= new Proceso(5,ip1,ip2,ip3);
				p2= new Proceso(6,ip1,ip2,ip3);
				p1.setName("Proceso 5");
				p2.setName("Proceso 6");
				break;	
		}
		
		p1.start();
		p2.start();
		
		System.out.println("Servidor "+server+" --> Procesos creados: "+p1.getName()+" y "+p2.getName());
		
	}

	
	
	/**Metodo para realizar la multidifusion de las peticiones a la SC a los demas procesos.
	 * @param T Sera el tiempo de Lamport del proceso que realiza la multidifusion.
	 * @param id Sera el identificador del proceso que realiza la multidifusion.
	 * @return
	 */
	@Path("peticion")
	@GET
	@Produces(MediaType.TEXT_PLAIN)

	public void peticionS (@QueryParam("T") int T, @QueryParam("id") int id){
		
		//Compruebo si el proceso que quiere realizar la multidifusion es mio.
		if(id==p1.id || id==p2.id){
			if(id==p1.id){
				p2.peticionP(T, id);
			}
			else{
				p1.peticionP(T, id);
			}
		}
		else{
			p1.peticionP(T, id);
			p2.peticionP(T, id);
		}
		
	}
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/
	

	
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/	
	/**Metodo que se ejecuta cuando un proceso responde para darte paso a la SC.
	 * @param id Identificador del proceso que envia la respuesta.
	 * @return
	 */
	@Path("respuesta")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	
	public void respuestaS(@QueryParam("id") int id){
		
		//Compruebo si es mi proceso
		if(id==p1.id){
			p1.respuestaP();
		}
		else if(id==p2.id){
			p2.respuestaP();
		}
		
	}
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/
	
	
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/
	@GET
	@Produces(MediaType.TEXT_PLAIN)	
	@Path("ntp")
	public String pedirtiempos(){
		
		t1 = System.currentTimeMillis();
		
		double time = Math.random()*1000+500;
		try {
			Thread.sleep((long)time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		t2 = System.currentTimeMillis();
		String t = t1+"-"+t2;
		return t;
	}
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/
	
	
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/
	@GET
	@Produces(MediaType.TEXT_PLAIN)	
	@Path("esperar")
	public void waitsemaforo(){
		
		try {
			sem.acquire(6);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("FIN");
	}
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/
	
	
	
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/
	@GET
	@Produces(MediaType.TEXT_PLAIN)	
	@Path("final")
	public void releasesemaforo(){
		
		sem.release();
		System.out.println("FIN");
		
	}
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/
	
	
	
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/
	@GET
	@Produces(MediaType.TEXT_PLAIN)	
	@Path("corregir")
	public void corregirtiempos(@QueryParam("offset") long offset) throws IOException{
		
		String cadena, escribir;
		String[] temp;
		String delimiter=" ";
		FileReader f = new FileReader("/home/i0922996/0.log");
		FileWriter fichero = null;
		PrintWriter pw = null;
		long num;
		
		if(p1.id==3 || p2.id==4){
			fichero = new FileWriter("/home/i0922996/1.log", true);
		}
		else{
			fichero = new FileWriter("/home/i0922996/2.log", true);
		}
		BufferedReader b = new BufferedReader(f);
		
		while((cadena = b.readLine())!=null){
			
			temp = cadena.split(delimiter);
			num = Long.parseLong(temp[2]) - offset;
			escribir = temp[0]+" "+temp[1]+" "+num;
			pw = new PrintWriter(fichero);
			pw.println(escribir);
			
		}
		fichero.close();
		b.close();
		
	}
	/*---------------------------------------------------------------------------------------------*/
	/*---------------------------------------------------------------------------------------------*/
}