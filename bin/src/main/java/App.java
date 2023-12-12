import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.Thread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.IntStream;


public class App {
    private static final String HTML_FILE_PATH = "html/prueba.html";

    public static void main(String[] args) {
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(new TuHttpHandler())
                .build();
        server.start();
    }

    private static Museo[] leerMuseosDesdeCSV() throws IOException {
        String rutaArchivo = "C:\\UnderTown\\maven\\bin\\Museos.csv";
        CsvMapper csvMapper = new CsvMapper();
        CsvSchema schema = CsvSchema.builder().setUseHeader(false).build();
        MappingIterator<Museo> iterator = csvMapper.readerFor(Museo.class).with(schema).readValues(new File(rutaArchivo));
    
        List<Museo> museosList = new ArrayList<>();
        while (iterator.hasNext()) {
            Museo museo = iterator.next();
            museosList.add(museo);
        }
    
        return museosList.toArray(new Museo[0]);
    }

    // Función para leer el archivo CSV y almacenar los datos en la lista de museos
    private static void readCSV(String csvFile, List<Museo> museos) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(csvFile));
        String line;

        // Leer el archivo línea por línea
        while ((line = br.readLine()) != null) {
            // Dividir la línea en campos utilizando la coma como delimitador
            String[] data = line.split(",");

            // Crear un objeto Museo y agregarlo a la lista
            Museo museo = new Museo(data[0], data[1], data[2], data[3], data[4], data[5]);
            museos.add(museo);
        }

        // Cerrar el BufferedReader
        br.close();
    }

    public static class TuHttpHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            String solicitudProcesadores = exchange.getQueryParameters().get("obtenerNucleos") != null
                                            ? exchange.getQueryParameters().get("obtenerNucleos").getFirst()
                                            : null;

            String mensajes = exchange.getQueryParameters().get("mensaje") != null
                                ? exchange.getQueryParameters().get("mensaje").getFirst()
                                : null;
            String processors = exchange.getQueryParameters().get("processors") != null
                                    ? exchange.getQueryParameters().get("processors").getFirst()
                                    : null;
            
            if (solicitudProcesadores != null){
                // Obtener la cantidad de procesadores
                int cantidadProcesadores = osBean.getAvailableProcessors();
            
                System.out.println("Nuevo cliente, procesadores disponibles: "+cantidadProcesadores);
                // Enviar la cantidad de procesadores como respuesta
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                exchange.getResponseSender().send("{\"cantidadNucleos\": " + cantidadProcesadores + "}");
                }

            if (mensajes != null) {
                System.out.println(processors);
                int nucleos = Integer.parseInt(processors);
                String[] Alcaldias = mensajes.split(",");
                List<Museo> Museos = new ArrayList<>();
                List<Museo> MuseosFiltrados = new ArrayList<>();
                String csvFile = "C:\\UnderTown\\maven\\bin\\Museos.csv";

                try {
                    readCSV(csvFile, Museos);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (Museo museo : Museos) {
                    if (Arrays.asList(Alcaldias).contains(museo.getMunNom())) {
                        MuseosFiltrados.add(museo);
                        //System.out.println(museo.getMuseoNombre());
                    }
                }

                int nClusters = 1;
                  // Variable para almacenar índices antes de construir clusters
                List<double[]> clusters = new ArrayList<>();
                List<Integer> ClusterPertenencia;
                
                boolean shouldIncrementClusters = false;
                

                do {
                    List<Integer> preClusters = new ArrayList<>();
                    clusters = new ArrayList<>();
                    boolean sameClusters = false;

                    shouldIncrementClusters = false;

                    //System.out.println("Cantidad de Clusteres: " + nClusters);

                    Random random = new Random(); //Museos aleatorios para iniciar los clusters
                    for (int i = 0; i < nClusters; i++) { 
                        int aux;
                        do {
                            aux = random.nextInt(MuseosFiltrados.size());
                        } while (preClusters.contains(aux));
                        preClusters.add(aux);
                    }

                    for (int i = 0; i < nClusters; i++) { //transformar la selección a sus coordenas cartesianas
                        int aux = preClusters.get(i);
                        double longitud = Double.parseDouble(MuseosFiltrados.get(aux).getGmapsLongitud());
                        double latitud = Double.parseDouble(MuseosFiltrados.get(aux).getGmapsLatitud());
                        clusters.add(new double[]{longitud, latitud});
                    }

                    for(double[] clus: clusters){ //imprimir los clusteres
                        //System.out.println(clus[0]+ ","+ clus[1]);
                    }

                    do {                        
                        // Realizar asignación de museos al clusters más cercano
                        ClusterPertenencia = asignarMuseosAClusters(MuseosFiltrados, clusters, nClusters);
                        for (int unCluster:ClusterPertenencia ){
                            //System.out.print(unCluster + " ");
                        }
                        //System.out.println();

                        // Nuevo centro de cada cluster
                        List<double[]> ClusterNuevoBaricentro = new ArrayList<>();
                        for (int i = 0; i < nClusters; i++) {
                            double sumaLongitud = 0;
                            double sumaLatitud = 0;
                            int count = 0;

                            for (int j = 0; j < MuseosFiltrados.size(); j++) {
                                if (ClusterPertenencia.get(j) == i) {
                                    double[] atributos = clusters.get(i);
                                    sumaLongitud += atributos[0];
                                    sumaLatitud += atributos[1];
                                    count++;
                                }
                            }

                            if (count > 0) {
                                double promedioLongitud = sumaLongitud / (double)count;
                                double promedioLatitud = sumaLatitud / (double)count;
                                double[] nuevoBaricentro = {promedioLongitud, promedioLatitud};
                                ClusterNuevoBaricentro.add(nuevoBaricentro);
                            }
                        }
                        //comprobar que los clusteres sean diferentes
                        for(double[] clus: clusters){
                            //System.out.println("viejo: "+ clus[0]+","+clus[1]);
                        }

                        for(double[] clus: ClusterNuevoBaricentro){
                            //System.out.println("nuevo: "+ clus[0]+","+clus[1]);
                        }

                        if (clusters.size() != ClusterNuevoBaricentro.size()) {
                            //System.out.println(clusters.size() + "," + ClusterNuevoBaricentro.size());
                            sameClusters=false;
                        }else{
                            for (int i = 0; i < clusters.size(); i++) {
                                sameClusters = true;
                                if (clusters.get(i)[0]!= ClusterNuevoBaricentro.get(i)[0]) {
                                    sameClusters=false;
                                }if (clusters.get(i)[1]!= ClusterNuevoBaricentro.get(i)[1]) {
                                    sameClusters=false;
                                }
                            }
                        }
                        clusters = ClusterNuevoBaricentro;

                    } while(!sameClusters);
                    
                    int max = Collections.max(ClusterPertenencia);
                    int[] numCluster = new int[max + 1];

                    for (int perteneceA:ClusterPertenencia){
                        numCluster[perteneceA]++;
                    }

                    if (Arrays.stream(numCluster).anyMatch(element -> element > 12)) {
                        nClusters += 1;
                        shouldIncrementClusters = true;
                    }

                }while (shouldIncrementClusters);
                
                System.out.println("Cantidad de Closteres: " + nClusters);
                /*for(double[] clus: clusters){
                    System.out.println("Closter final: "+ clus[0]+","+clus[1]);
                }*/

                //Asignación de los museos a los clusteres finales para encontrar la menor ruta entre ellos
                List<List<Museo>> MuseosClusterizados = new ArrayList<>();
                List<List<Museo>> rutaDeCluster = new ArrayList<>();
                for (int i=0; i<nClusters; i++){
                    List<Museo> clusteri = new ArrayList<>();
                    List<Museo> rutita = new ArrayList<>();
                    for(int j=0; j<ClusterPertenencia.size(); j++){
                        if (ClusterPertenencia.get(j) == i){
                            clusteri.add(MuseosFiltrados.get(j));
                        }
                    }
                    rutaDeCluster.add(rutita);
                    MuseosClusterizados.add(clusteri);
                }

                //Inicialización de los nucleos
                //inicio del tiempo
                long tiempoInicio = System.currentTimeMillis();
                //Asignación de la busqueda por nucleos
                /*GrafoCluster[] BusquedaTotal = new GrafoCluster[nClusters];
                for(int i=0; i<nClusters; i++){
                    List<Museo> museosRestantes = new ArrayList<>(MuseosClusterizados.get(i));
                    Museo museoInicio = museosRestantes.get(0);
                    museosRestantes.remove(0);
                    GrafoCluster clustercito = new GrafoCluster(museosRestantes, museoInicio, rutaDeCluster.get(i));
                    clustercito.run();
                    BusquedaTotal[i] = clustercito;
                }
                for(GrafoCluster clustercito: BusquedaTotal){
                    clustercito.join();
                }
                */
                //fin del tiempo
                long tiempoFin = System.currentTimeMillis();
                // Calcular la diferencia de tiempo
                long tiempoTotal = tiempoFin - tiempoInicio;
                // Imprimir el tiempo total en milisegundos
                //System.out.println("Tiempo de ejecución paralelo: " + tiempoTotal + " milisegundos");

                //Inicialización de los nucleosSecuenciales
                //inicio del tiempo
                tiempoInicio = System.currentTimeMillis();
                //Asignación de la busqueda por nucleos
                GrafoNucleo[] BusquedaRapida = new GrafoNucleo[nClusters];
                for(int i=0; i<nClusters; i++){
                    List<Museo> museosRestantes = new ArrayList<>(MuseosClusterizados.get(i));
                    Museo museoInicio = museosRestantes.get(0);
                    museosRestantes.remove(0);
                    GrafoNucleo clustercito = new GrafoNucleo(museosRestantes, museoInicio, rutaDeCluster.get(i));
                    clustercito.start();
                    BusquedaRapida[i] = clustercito;
                }
                for(GrafoNucleo clustercito: BusquedaRapida){
                    clustercito.join();
                }
                //fin del tiempo
                tiempoFin = System.currentTimeMillis();
                // Calcular la diferencia de tiempo
                tiempoTotal = tiempoFin - tiempoInicio;
                // Imprimir el tiempo total en milisegundos
                System.out.println("Tiempo de ejecución paralelo-secuancial: " + tiempoTotal + " milisegundos");

                // Versión secuencial
                tiempoInicio = System.currentTimeMillis();
                GrafoSecuencial[] AlgoritmoSecuencial = new GrafoSecuencial[nClusters];
                for(int i=0; i<nClusters; i++){
                    List<Museo> museosRestantes = new ArrayList<>(MuseosClusterizados.get(i));
                    Museo museoInicio = museosRestantes.get(0);
                    museosRestantes.remove(0);
                    GrafoSecuencial grafito = new GrafoSecuencial(museosRestantes, museoInicio, rutaDeCluster.get(i));
                    grafito.BusquedaCompleta();
                }
                //fin del tiempo
                tiempoFin = System.currentTimeMillis();
                // Calcular la diferencia de tiempo
                tiempoTotal = tiempoFin - tiempoInicio;
                // Imprimir el tiempo total en milisegundos
                System.out.println("Tiempo de ejecución secuencial: " + tiempoTotal + " milisegundos");


                List<Object[]> FIN = new ArrayList<>();
                for(GrafoNucleo hilo:BusquedaRapida){
                    FIN.add( hilo.MejorRecorrido());
                }
                ObjectMapper objectMapper = new ObjectMapper();
                // Crear un array JSON para almacenar los resultados
                ArrayNode resultadosNode = objectMapper.createArrayNode();
                for(Object[] caminito:FIN){
                    ObjectNode resultado = crearResultadoNode(objectMapper, (Double)caminito[0], (List<Museo>)caminito[1]);
                    resultadosNode.add(resultado);
                }
                // Convertir el array JSON a una cadena JSON
                String responseMessage = resultadosNode.toString();

                //Inicio de la respuesta al servidor
                /*StringBuilder responseBuilder = new StringBuilder("Nuevos Baricentros: ");
                for (double[] baricentro : clusters) {
                    for (double value : baricentro) {
                        responseBuilder.append(String.format("%.1f", value)).append(", ");
                    }
                }

                responseBuilder.deleteCharAt(responseBuilder.length() - 2);
                
                String responseMessage = responseBuilder.toString();
                */
                // Configurar las cabeceras de la respuesta
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=UTF-8");
                exchange.getResponseSender().send(responseMessage); //Envio de la respuesta
            } else {
                String htmlContent = readHtmlFile();
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8");
                exchange.getResponseSender().send(htmlContent);
            }
        }

        private List<Integer> asignarMuseosAClusters(List<Museo> MuseosFiltrados, List<double[]> clusters, int nClusters) {
            List<Integer> ClusterPertenencia = new ArrayList<>();
            for (Museo museo : MuseosFiltrados) {
                double distancia = -1;
                int clusterPertenece = 0;

                for (int i = 0; i < nClusters; i++) {
                    double disaux = Math.pow(Double.parseDouble(museo.getGmapsLongitud()) - clusters.get(i)[0], 2)
                            + Math.pow(Double.parseDouble(museo.getGmapsLatitud()) - clusters.get(i)[1], 2);

                    if (distancia > disaux || distancia == -1) {
                        distancia = disaux;
                        clusterPertenece = i;
                    }
                }
                ClusterPertenencia.add(clusterPertenece);
            }
            return ClusterPertenencia;
        }
        private ObjectNode crearResultadoNode(ObjectMapper objectMapper, Double distancia, List<Museo> ruta) {
            ObjectNode resultadoNode = objectMapper.createObjectNode();
            Boolean saltar = true;
            resultadoNode.put("distancia", distancia);
        
            ArrayNode rutaNode = objectMapper.createArrayNode();
            for (Museo museo : ruta) {
                //System.out.println(museo.getMuseoNombre());
                if (saltar || ruta.size()==1){
                    saltar=false;
                }else{
                    ObjectNode museoNode = objectMapper.createObjectNode();
                    museoNode.put("museo_id", museo.getMuseoId());
                    museoNode.put("museo_nombre", museo.getMuseoNombre());
                    museoNode.put("gmaps_latitud", museo.getGmapsLatitud());
                    museoNode.put("gmaps_longitud", museo.getGmapsLongitud());
                    museoNode.put("municipio_id", museo.getMunicipioId());
                    museoNode.put("nom_mun", museo.getMunNom());
            
                    rutaNode.add(museoNode);
                }
            }
        
            resultadoNode.set("ruta", rutaNode);
        
            return resultadoNode;
        }
    }

    private static String readHtmlFile() {
        try (InputStream inputStream = App.class.getClassLoader().getResourceAsStream(HTML_FILE_PATH)) {
            if (inputStream != null) {
                try (Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A")) {
                    return scanner.hasNext() ? scanner.next() : "";
                }
            } else {
                return "HTML file not found";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error leyendo archivo HTML principal: debe estar en src\\resources\\html";
        }
    }

    public static class GrafoNucleo extends Thread {
        private List<Museo> museosRestantes;
        private Museo Inicio;
        private List<Museo> ruta;
        private Object[] FIN;   

        public GrafoNucleo(List<Museo> museosRestantes, Museo Inicio, List<Museo> ruta) {
            this.museosRestantes = museosRestantes;
            this.Inicio = Inicio;
            this.ruta = ruta;
            this.FIN = new Object[2];
        }

        public Object[] MejorRecorrido(){
            return this.FIN;
        }
        
        @Override
        public void run(){
            if(museosRestantes.size()==0){
                this.FIN[0] = (Double)0.0;
                this.FIN[1] = new ArrayList<Museo>(Arrays.asList(Inicio));
            }else{

                int tamaño = museosRestantes.size();
                Double[] distancias = new Double[tamaño];
                List<Museo>[] rutas = new ArrayList[tamaño];
                GrafoNucleo[] auxClusters1 = new GrafoNucleo[tamaño];
                GrafoSecuencial[] auxClusters2 = new GrafoSecuencial[tamaño];
                //System.out.println(tamaño);

                if (tamaño>8){
                    for(int i=0; i<tamaño; i++){
                        List<Museo> auxMuseosRestantes = new ArrayList<>(this.museosRestantes);
                        Museo auxInicial = auxMuseosRestantes.get(i);
                        List<Museo> auxRuta = new ArrayList<>();
                        auxMuseosRestantes.remove(i);

                        distancias[i] = Math.pow(
                                        Math.pow(Double.parseDouble(this.Inicio.getGmapsLatitud()) - 
                                        Double.parseDouble(auxInicial.getGmapsLatitud()),   2) +
                                        Math.pow(Double.parseDouble(this.Inicio.getGmapsLongitud()) -
                                        Double.parseDouble(auxInicial.getGmapsLongitud()),   2)
                                        , 0.5);

                        GrafoNucleo auxCluster = new GrafoNucleo(auxMuseosRestantes, auxInicial, auxRuta);
                        auxCluster.start();
                        auxClusters1[i] = auxCluster;
                       
                    }
                }else{
                    for(int i=0; i<tamaño; i++){
                        List<Museo> auxMuseosRestantes = new ArrayList<>(this.museosRestantes);
                        Museo auxInicial = auxMuseosRestantes.get(i);
                        List<Museo> auxRuta = new ArrayList<>();
                        auxMuseosRestantes.remove(i);

                        distancias[i] = Math.pow(
                                        Math.pow(Double.parseDouble(this.Inicio.getGmapsLatitud()) -     
                                        Double.parseDouble(auxInicial.getGmapsLatitud()),   2) +
                                        Math.pow(Double.parseDouble(this.Inicio.getGmapsLongitud()) -
                                        Double.parseDouble(auxInicial.getGmapsLongitud()),   2)
                                        , 0.5);

                        GrafoSecuencial auxCluster = new GrafoSecuencial(auxMuseosRestantes, auxInicial, auxRuta);
                        auxCluster.BusquedaCompleta();
                        auxClusters2[i] = auxCluster;
                    }
                }
                for(int i=0; i<tamaño; i++){
                    Object[] resultadoBusqueda = new Object[2];
                    if (tamaño>8){
                        try{
                            auxClusters1[i].join();
                        }catch(InterruptedException e) {
                            // Manejar la excepción, por ejemplo, imprimir el seguimiento de la pila
                            e.printStackTrace();
                        }
                    }
                    resultadoBusqueda = (tamaño>8)?auxClusters1[i].MejorRecorrido():auxClusters2[i].MejorRecorrido();
                    distancias[i] += (Double)resultadoBusqueda[0];
                    rutas[i] = (List<Museo>)resultadoBusqueda[1];
                }

                double minDistancia = distancias[0];
                int minI=0;
                for(int i=1; i<tamaño; i++){
                    if (minDistancia>=distancias[i]) {
                        minDistancia=distancias[i];
                        minI=i;
                    }
                }
                rutas[minI].add(this.museosRestantes.get(minI));
                this.FIN[0] = distancias[minI];
                this.FIN[1] = rutas[minI];
                //System.out.println(this.FIN[0]+","+this.FIN[1]);
            }
        }
    }

    public static class GrafoCluster extends Thread{
        private List<Museo> museosRestantes;
        private Museo Inicio;
        private List<Museo> ruta;
        private Object[] FIN;

        public GrafoCluster(){}

        public GrafoCluster(List<Museo> museosRestantes, Museo Inicio, List<Museo> ruta){
            this.museosRestantes = museosRestantes;
            this.Inicio = Inicio;
            this.ruta = ruta;
            this.FIN = new Object[2];
        }

        public Object[] MejorRecorrido(){
            return this.FIN;
        }

        @Override
        public void run(){
            if(museosRestantes.size()==0){
                this.FIN[0] = (Double)0.0;
                this.FIN[1] = new ArrayList<Museo>(Arrays.asList(Inicio));
            }else{

                int tamaño = museosRestantes.size();
                Double[] distancias = new Double[tamaño];
                List<Museo>[] rutas = new ArrayList[tamaño];
                GrafoCluster[] auxClusters = new GrafoCluster[tamaño];

                for(int i=0; i<tamaño; i++){
                    List<Museo> auxMuseosRestantes = new ArrayList<>(this.museosRestantes);
                    Museo auxInicial = auxMuseosRestantes.get(i);
                    List<Museo> auxRuta = new ArrayList<>();
                    auxMuseosRestantes.remove(i);

                    distancias[i] = Math.pow(
                                    Math.pow(Double.parseDouble(this.Inicio.getGmapsLatitud()) - 
                                    Double.parseDouble(auxInicial.getGmapsLatitud()),   2) +
                                    Math.pow(Double.parseDouble(this.Inicio.getGmapsLongitud()) -
                                    Double.parseDouble(auxInicial.getGmapsLongitud()),   2)
                                    , 0.5);

                    GrafoCluster auxCluster = new GrafoCluster(auxMuseosRestantes, auxInicial, auxRuta);
                    auxCluster.run();
                    auxClusters[i] = auxCluster;
                }
                for(int i=0; i<tamaño; i++){
                    Object[] resultadoBusqueda = new Object[2];
                    try{
                        auxClusters[i].join();
                    }catch(InterruptedException e) {
                        // Manejar la excepción, por ejemplo, imprimir el seguimiento de la pila
                        e.printStackTrace();
                    }
                    resultadoBusqueda = auxClusters[i].MejorRecorrido();
                    distancias[i] += (Double)resultadoBusqueda[0];
                    rutas[i] = (List<Museo>)resultadoBusqueda[1];
                }

                double minDistancia = distancias[0];
                int minI=0;
                for(int i=1; i<tamaño; i++){
                    if (minDistancia>=distancias[i]) {
                        minDistancia=distancias[i];
                        minI=i;
                    }
                }
                rutas[minI].add(this.museosRestantes.get(minI));
                this.FIN[0] = distancias[minI];
                this.FIN[1] = rutas[minI];
                //System.out.println(this.FIN[0]+","+this.FIN[1]);
            }
        }
    }

    public static class GrafoSecuencial{
        private List<Museo> museosRestantes;
        private Museo Inicio;
        private List<Museo> ruta;
        private Object[] FIN;

        public GrafoSecuencial(){}

        public GrafoSecuencial(List<Museo> museosRestantes, Museo Inicio, List<Museo> ruta){
            this.museosRestantes = museosRestantes;
            this.Inicio = Inicio;
            this.ruta = ruta;
            this.FIN = new Object[2];
        }

        public Object[] MejorRecorrido(){
            return this.FIN;
        }

        public void BusquedaCompleta(){
            if(museosRestantes.size()==0){
                this.FIN[0] = (Double)0.0;
                this.FIN[1] = new ArrayList<Museo>(Arrays.asList(Inicio));
            }else{

                int tamaño = museosRestantes.size();
                Double[] distancias = new Double[tamaño];
                List<Museo>[] rutas = new ArrayList[tamaño];
                GrafoSecuencial[] auxClusters = new GrafoSecuencial[tamaño];

                for(int i=0; i<tamaño; i++){
                    List<Museo> auxMuseosRestantes = new ArrayList<>(this.museosRestantes);
                    Museo auxInicial = auxMuseosRestantes.get(i);
                    List<Museo> auxRuta = new ArrayList<>();
                    auxMuseosRestantes.remove(i);

                    distancias[i] = Math.pow(
                                    Math.pow(Double.parseDouble(this.Inicio.getGmapsLatitud()) - 
                                    Double.parseDouble(auxInicial.getGmapsLatitud()),   2) +
                                    Math.pow(Double.parseDouble(this.Inicio.getGmapsLongitud()) -
                                    Double.parseDouble(auxInicial.getGmapsLongitud()),   2)
                                    , 0.5);

                    GrafoSecuencial auxCluster = new GrafoSecuencial(auxMuseosRestantes, auxInicial, auxRuta);
                    auxCluster.BusquedaCompleta();
                    auxClusters[i] = auxCluster;
                }
                for(int i=0; i<tamaño; i++){
                    Object[] resultadoBusqueda = new Object[2];
                    resultadoBusqueda = auxClusters[i].MejorRecorrido();
                    distancias[i] += (Double)resultadoBusqueda[0];
                    rutas[i] = (List<Museo>)resultadoBusqueda[1];
                }

                double minDistancia = distancias[0];
                int minI=0;
                for(int i=1; i<tamaño; i++){
                    if (minDistancia>=distancias[i]) {
                        minDistancia=distancias[i];
                        minI=i;
                    }
                }
                rutas[minI].add(this.museosRestantes.get(minI));
                this.FIN[0] = distancias[minI];
                this.FIN[1] = rutas[minI];
                //System.out.println(this.FIN[0]+","+this.FIN[1]);
            }
        }
    }

    // Clase modelo para mapear las filas del CSV
    public static class Museo {
        private String museo_id;
        private String museo_nombre;
        private String gmaps_latitud;
        private String gmaps_longitud;
        private String municipio_id;
        private String nom_mun;

        // Constructor por defecto
        public Museo() {}

        // Constructor con parámetros
        public Museo(String museo_id, String museo_nombre, String gmaps_latitud, String gmaps_longitud, String municipio_id, String nom_mun) {
            this.museo_id = museo_id;
            this.museo_nombre = museo_nombre;
            this.gmaps_latitud = gmaps_latitud;
            this.gmaps_longitud = gmaps_longitud;
            this.municipio_id = municipio_id;
            this.nom_mun = nom_mun;
        }

        // Getters y setters
        public String getMuseoId() {
            return museo_id;
        }

        public void setMuseoId(String museo_id) {
            this.museo_id = museo_id;
        }

        public String getMuseoNombre() {
            return museo_nombre;
        }

        public void setMuseoNombre(String museo_nombre) {
            this.museo_nombre = museo_nombre;
        }

        public String getGmapsLatitud() {
            return gmaps_latitud;
        }

        public void setGmapsLatitud(String gmaps_latitud) {
            this.gmaps_latitud = gmaps_latitud;
        }

        public String getGmapsLongitud() {
            return gmaps_longitud;
        }

        public void setGmapsLongitud(String gmaps_longitud) {
            this.gmaps_longitud = gmaps_longitud;
        }

        public String getMunicipioId() {
            return municipio_id;
        }

        public void setMunicipioId(String municipio_id) {
            this.municipio_id = municipio_id;
        }

        public String getMunNom() {
            return nom_mun;
        }

        public void setMunNom(String nom_mun) {
            this.nom_mun = nom_mun;
        }
    }
}
