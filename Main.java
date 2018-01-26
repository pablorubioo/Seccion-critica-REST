package principal;

import java.net.URI;
import java.util.concurrent.Semaphore;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;


/*
*****************************************************************************************
*------------->INTRODUCIR LAS IPS DE LAS 3 MAQUINAS POR LOS ARGUMENTOS DEL PROGRAMA		*
*------------->MAQUINA REFERENCIA --> ARGS[0]											*	
*																						*
* La ruta de los log es en este caso, /home/i0922996									*
*																						*
*****************************************************************************************
*/

public class Main {

	private static String[] ips = new String[3];
	private static WebTarget[] target = new WebTarget[3];
	private static int num_maquinas = 3;
	
public static void main(String[] args){

	
	long mediaA[] = new long[2];
	long mediaB[] = new long[2];
	long mejorParA1[] = new long[2];
	long mejorParA2[] = new long[2];
	long mejorParB1[] = new long[2];
	long mejorParB2[] = new long[2];

	
	//Comprobaciones sobre los argumentos.
	if(args.length!=3){
		System.out.println("Debes introducir las ips de las 3 maquinas como argumentos.");
		System.out.println("La maquina de referencia será la primera introducida.");
		System.exit(0);
	}
	
	//Creamos los target a partir de las ips obtenidas como argumento.
	for(int i=0;i<num_maquinas;i++){
		ips[i]=args[i];
		target[i]=Conectar(ips[i]);
	}
	
	
	//Primeras 10 ejecuciones de NTP. Guardamos el mejor par de las otras 2 máquinas.
    mejorParA1=ntp(1);    	
    mejorParB1=ntp(2);
	
    
	//Creacion de los procesos a través del servidor
    for (int i=0; i<num_maquinas;i++){
    	char s = (char) (i+65);
    	target[i].path("rest").path("despachador/crearProcesos").queryParam("servidor", s)
    	.queryParam("ip1", ips[0])
    	.queryParam("ip2", ips[1])
    	.queryParam("ip3", ips[2])
    	.request(MediaType.TEXT_PLAIN).get(String.class);
    }
	System.out.println("MAIN: Espero a que se termine la ejecucion de los procesos.");

    //Esperar a que se terminen las iteraciones de los procesos
	target[0].path("rest").path("despachador/esperar").request(MediaType.TEXT_PLAIN).get(String.class);
	System.out.println("MAIN: Fin de los procesos.");
		
	
	//Ultimas 10 ejecuciones de NTP. Volvemos a guardar el mejor par de las otras 2 máquinas.
	mejorParA2=ntp(1);    	
    mejorParB2=ntp(2);   	
	 
	//Calculamos la estimacion del mejorPar
    mediaA=mediaPares(mejorParA1,mejorParA2);
    mediaB=mediaPares(mejorParB1,mejorParB2);
    System.out.println("Mejor par con la maquina 1: Delay-> "+mediaA[0]+" Offset-> "+mediaA[1]);
    System.out.println("Mejor par con la maquina 2: Delay-> "+mediaB[0]+" Offset-> "+mediaB[1]);


	//Corregimos tiempos del log
	target[1].path("rest").path("despachador/corregir").queryParam("offset", mediaA[1]).request(MediaType.TEXT_PLAIN).get(String.class);
	target[2].path("rest").path("despachador/corregir").queryParam("offset", mediaB[1]).request(MediaType.TEXT_PLAIN).get(String.class);
	
	 
	System.out.println("FIN DEL MAIN.");
	
}



/**Metodo para la creacion del target del servidor cuya ip nos pasan como argumento.
 * @param ip Direccion ip del target.
 * @return Target ya creado.
 */
public static WebTarget Conectar(String ip) {
    Client client = ClientBuilder.newClient();
    URI uri = UriBuilder.fromUri("http://" + ip + ":8080/ObligatoriaDistribuidos/").build();

    WebTarget target = client.target(uri);
    return target;
}

/**Metodo que devuelve el mejor par de tiempos de la maquina que se le pasa como argumento.
 * @param maquina Marca la posicion de la maquina en el array de targets.
 * @return
 */
public static long[] ntp(int maquina){
	
	long mejorPar[] = new long[2];
	double inf= Double.POSITIVE_INFINITY;

	long t0, t1, t2, t3, offset, delay;
	
	String delimiter = "-";
	String[] temp;
	
	//Iniciar mejor par a infinito.
	mejorPar[0]=(long) inf;
	mejorPar[1]=(long) inf;

		for(int i=0;i<10;i++){
			
			t0=System.currentTimeMillis();
			
			String respuesta = target[maquina].path("rest").path("despachador/ntp").request(MediaType.TEXT_PLAIN).get(String.class);
			temp = respuesta.split(delimiter);
			
			t1=Long.parseLong(temp[0]);
			t2=Long.parseLong(temp[1]);
			System.out.println(t1+"   "+t2);
			
			t3=System.currentTimeMillis();
			
			delay = ((t1-t0) + (t3-t2));
			offset = (t1-t0 + t2-t3)/2;
			
			if(delay < mejorPar[0]){
				mejorPar[0]=delay;
				mejorPar[1]=offset;
				}
			}

		return mejorPar;
}

/**Obtiene la media de los pares que se le pasan como argumento.
 * @param p1 el primer par.
 * @param p2 el segundo par.
 * @return
 */
public static long[] mediaPares(long[] p1, long[] p2){
	long par[] = new long[2];

	par[0]=(p1[0]+p2[0])/2;
	par[1]=(p1[1]+p2[1])/2;
	
	return par;
}


}