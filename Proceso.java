package principal;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

public class Proceso extends Thread{

	int id, num_maquinas=3, C,T;
	long tiempo;
	String estado="";
    Semaphore semaforo = new Semaphore(0);
    public static int numprocesos = 6;
    ConcurrentLinkedQueue<Integer> cola = new ConcurrentLinkedQueue<Integer>();
    
    //Tablas con las direcciones de las maquinas.
    public static String[] ips = new String[3];
    public static WebTarget[] target = new WebTarget[3];
	  
    
	
public Proceso (int id, String ip1, String ip2, String ip3){
	this.id=id;
    ips[0] = ip1;
    ips[1] = ip2;
    ips[2] = ip3;
    target[0] = Conectar(ips[0]);
    target[1] = Conectar(ips[1]);
    target[2] = Conectar(ips[2]);
}

/*
 * Metodo principal que ejecutara cada uno de los hilos al invocar el metodo start().
 */	
public void run(){
	
		estado = "LIBERADA";
		C=0;
		
		for(int i=0; i<100; i++){
			
			//Primer sleep
			try {
				Thread.sleep((long)Math.random()*200+300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Intento entrar en la seccion critica
			estado = "BUSCADA";
		    T=C;
			seccionCritica();
				
		}//for
		
		target[0].path("rest").path("despachador/final").request(MediaType.TEXT_PLAIN).get(String.class);
		
		System.out.println("Fin del proceso "+this.id);
		}




private void seccionCritica() {
	//Multidifusion de peticion <T,id_proceso> de entrada de la SC
	for(int i=0;i<num_maquinas;i++){
	    target[i].path("rest").path("despachador/peticion").queryParam("T", T)
		.queryParam("id", id)
		.request(MediaType.TEXT_PLAIN).get(String.class);
	}   
	//Esperar por las respuestas de todos los procesos
	try {
		System.out.println("Soy:"+this.id+" antes del semaforo.");
		semaforo.acquire(numprocesos-1);
		System.out.println("Soy:"+this.id+" despues del semaforo.");
	} catch (InterruptedException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}

	//Entrada a la seccion critica
	synchronized(this.getClass()){
		estado = "TOMADA";
		C++;
		tiempo = System.currentTimeMillis();
		this.write_log(id, "E", tiempo);
		System.out.println("Soy <T:"+this.T+", ID:"+this.id+">. Entro a la SC");
		
		//Segundo sleep
		try {
			Thread.sleep((long)Math.random()*200+100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		tiempo = System.currentTimeMillis();
		this.write_log(id, "S", tiempo);

		}//Fin de la seccion critica
		estado = "LIBERADA";
	
		//Responder peticiones en cola hasta que esta se vacie
		while(!cola.isEmpty()){
			int a = cola.remove();
			System.out.println("Soy <T:"+this.T+", ID:"+this.id+">. Mando mensajes a los de mi cola. A Proceso "+a);
			if(a==1 || a==2){
				target[0].path("rest").path("despachador/respuesta").queryParam("id", a)
				.request(MediaType.TEXT_PLAIN).get(String.class);
			}
			else if(a==3 || a==4){
				target[1].path("rest").path("despachador/respuesta").queryParam("id", a)
				.request(MediaType.TEXT_PLAIN).get(String.class);
			}
			else{
				target[2].path("rest").path("despachador/respuesta").queryParam("id", a)
				.request(MediaType.TEXT_PLAIN).get(String.class);
			}
		}
		
}

/*---------------------------------------------------------------------------------------------*/
/*---------------------------------------------------------------------------------------------*/
/**Metodo para automatizar las conexiones a diferentes ips.
 * @param ip Guarda el valor de ip a la que queremos conectarnos.
 * @return target de dicha ip.
 */
public static WebTarget Conectar(String ip) {
    Client client = ClientBuilder.newClient();
    URI uri = UriBuilder.fromUri("http://" + ip + ":8080/ObligatoriaDistribuidos/").build();

    WebTarget target = client.target(uri);
    return target;
}

/*---------------------------------------------------------------------------------------------*/
/*---------------------------------------------------------------------------------------------*/
/**
 * Metodo para escribir los ficheros de log.
 * @param id Sera el numero de identificacion del proceso que escribe.
 * @param x Guarda el valor E o S dependiendo de si entra o sale de la SC.
 * @param tiempo Valor del tiempo que debe escribir en el fichero de log.
 */
public void write_log(int id, String x, long tiempo){
		FileWriter fichero = null;
		PrintWriter pw = null;
		
		try {
			
			fichero = new FileWriter("/home/i0922996/0.log", true);
			
			pw = new PrintWriter(fichero);
			pw.println("P"+id+" "+x+" "+ tiempo);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
				try {
					if(null != fichero){
					fichero.close();
					}
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
		}

/*---------------------------------------------------------------------------------------------*/
/*---------------------------------------------------------------------------------------------*/
/**Metodo que se ejecuta al recibir una peticion de otro proceso para entrar en la SC.
 * @param T_recibido Valor del tiempo del Lamport del proceso que nos manda la peticion.
 * @param proceso_pideSC Id del proceso que nos manda la peticion.
 */
public void peticionP (int T_recibido, int proceso_pideSC){
	//Damos a C el valor maximo entre C y T y le sumamos 1.
	if(C>T){
		C=C+1;
	}else{
		C=T+1;
		}
	System.out.println("Soy <T:"+this.T+", ID:"+this.id+",E: "+this.estado+">. Peticion recibida de <T:"+T_recibido+", ID:"+proceso_pideSC+">.");
	
	if(this.estado=="TOMADA" || this.estado=="BUSCADA" && this.T<T_recibido){
		//Tengo prioridad->Encolar.
		System.out.println("Lo meto a la cola. Yo <T:"+this.T+", ID:"+this.id+">. El otro <T:"+T_recibido+", ID:"+proceso_pideSC+">.");
		cola.add(proceso_pideSC);
		
	}else if(this.estado=="BUSCADA" && this.T==T_recibido){
		
		//Si T==tiempo se comparan los id de los procesos y tiene preferencia el de menor id.
		if(this.id < proceso_pideSC){
			//Como mi id es menor tengo prioridad->encolar
				
			System.out.println("Lo meto a la cola. Yo <T:"+this.T+", ID:"+this.id+">. El otro <T:"+T_recibido+", ID:"+proceso_pideSC+">.");
			cola.add(proceso_pideSC);
		}else{
			//Como mi id es mayor->respondo directamente
			System.out.println("Respondo directamente. Yo <T:"+this.T+",ID:"+this.id+">. El otro<T:"+T_recibido+", ID:"+proceso_pideSC+">.");
			
			if(proceso_pideSC==1 || proceso_pideSC==2){
				target[0].path("rest").path("despachador/respuesta").queryParam("id", proceso_pideSC)
				.request(MediaType.TEXT_PLAIN).get(String.class);
			}
			else if(proceso_pideSC==3 || proceso_pideSC==4){
				target[1].path("rest").path("despachador/respuesta").queryParam("id", proceso_pideSC)
				.request(MediaType.TEXT_PLAIN).get(String.class);
			}
			else{
				target[2].path("rest").path("despachador/respuesta").queryParam("id", proceso_pideSC)
				.request(MediaType.TEXT_PLAIN).get(String.class);
			}
		}	
	}else{
		//Respondo directamente
		System.out.println("Respondo directamente. Yo <T:"+this.T+",ID:"+this.id+">. El otro<T:"+T_recibido+", ID:"+proceso_pideSC+">.");
			
		if(proceso_pideSC==1 || proceso_pideSC==2){
			target[0].path("rest").path("despachador/respuesta").queryParam("id", proceso_pideSC)
			.request(MediaType.TEXT_PLAIN).get(String.class);
		}
		else if(proceso_pideSC==3 || proceso_pideSC==4){
			target[1].path("rest").path("despachador/respuesta").queryParam("id", proceso_pideSC)
			.request(MediaType.TEXT_PLAIN).get(String.class);
		}
		else{
			target[2].path("rest").path("despachador/respuesta").queryParam("id", proceso_pideSC)
			.request(MediaType.TEXT_PLAIN).get(String.class);
			}
		}
		
	
	
	}

/*---------------------------------------------------------------------------------------------*/
/*---------------------------------------------------------------------------------------------*/
/**Metodo para gestionar las respuestas de los demas metodos a tu peticion de entrada a la seccion critica.
 * @param proceso_responde Identificador del proceso que te ha respondido.
 * @param T_responde Tiempo de Lamport del proceso que te ha respondido.
 */
public void respuestaP(){
		
	semaforo.release();
	System.out.println("Soy <T:"+this.T+", ID:"+this.id+">. Respuestas recibidas");
}
/*---------------------------------------------------------------------------------------------*/
/*---------------------------------------------------------------------------------------------*/


}