# El Mejor camino entre museos

Saber cuál es la ruta de menor distancia entre $n$ museos deja de ser facil cuando $n>3$. Basado en esta problematica se busca saber cuál es la ruta más corta entre los museos de la CDMX, trazando lineas rectas entre cada par de ellos y usando por distancia la distancia uclideana con sus coordenadas geograficas.

---
## Universidad Nacional Autonoma de México
### Facultad de Estudios Superiores Acatlán
### Licenciatura en Ciencia de Datos
### Jerónimo Jahir Avendaño Pachuca
#### Bajo la supervisión de
### Mtro. Pablo Martínez Castro
---
## Indice
1. [¿Cómo sabemos que es la mejor ruta?](#Algo1)
    1. [Busqueda Completa](#Algo1.1)
    2. [Clusters](#Algo1.2)
2. [Implementación de la Busqueda Completa](#Algo2)
3. [Implementación de la asignación por Cluster](#Algo3)
4. [Paralelización](#Algo4)
5. [Tiempo Secuencial vs Paralelo](#Algo5)
6. [Descarga del Repositorio](#Algo6)
   1. [Compilar](#Algo6.1)
   2. [Ejecutar](#Algo6.2)
---
## ¿Cómo sabemos que es la mejor ruta? <a name="Algo1"></a>
Si tenemos dos museos que visitar, digamos A y B, contamos con 2 formas de hacerlo:
- Empezamos en A y terminamos en B
- Empezamos en B y terminamos en A

Si contamos con tres museos a los que visitar, A, B, C; se cuentan con 6 formas de hacerlo:
- Empezar en A y recorrer los 2 restantes (2 formas)
- Empezar en B y recorrer los 2 restantes (2 formas)
- Empezar en C y recorrer los 2 restantes (2 formas)

Si contamos con conocimientos en combinatoria, vemos que esto va creciendo a un ritmo de $n!$. Por ello es un algoritmo en donde se puede obtener una gran ganacia en terminos de eficiencia si usamos el paradigma concurrente.

**¿Cómo?**
### Busqueda Completa <a name="Algo1.1"></a>
Se toma un museo, digamos $m_1$, y se obtiene la distancia que hay de $m_1$ a los demás museos, luego para cada museo que resta se obtiene la distancia con sin él y $m_1$, así sucesivamente hasta que solo quede un museo, cuya distancia a sí mismo es 0, regresando la distancia recorrida y el orden en que fueron visitados los museos.

Comparamos la distancia total recorrida en cada ruta completa de $m_1$ a los demás museos y guardamos la ruta con la menor distancia.
De este modo aseguramos recuperar en cada subbusqueda la menor de las rutas.

Sabemos que $n!$ es una de las mayores complejidades en notación asintotica  por lo que con $n=10$, ya contamos con $3,628,000$ posibles caminos que evaluar. Aunque una máquina sea capaz de resolver esta tarea en milisegundo, con aumentar la cantidad de museos a $12$ pasaría a tardarse muchos minutos y con $14$ ya son horas. Es necesario encontrar otra forma de abordar este problema para no hacer una busqueda completa sobre los $182$ museos que tiene la CDMX, para ello se recurrio al algoritmo de Clusterización.

### Clusters <a name="Algo1.2"></a>
O grupos, por su traducción al español, es una estrategia para separar y dividir un conjunto con muchos elementos. La idea es la sigueinte:
- Definir la cantidad de Clusters
- Seleccionar al azar tantos nodos como Clusters haya, cada uno de ellos será el centro del cluster
1. Para cada museo encontrar que centro de cluster le queda más cerca
2. Asignar el nuevo centro de cada cluster a partir de los nodos contenidos en él.
3. Si el centro del cluster no cambio con respecto a la iteración anterior, entonces termina el algoritmos, si cambia se repite desde el paso 1.

## Implementación de la Busqueda Completa <a name="Algo2"></a>
~~~java
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
~~~
## Implementación de la asignación por Cluster <a name="Algo3"></a>
~~~java
do {
    List<Integer> preClusters = new ArrayList<>();
    clusters = new ArrayList<>();
    boolean sameClusters = false;
    shouldIncrementClusters = false;
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

    do {                        
        // Realizar asignación de museos al clusters más cercano
        ClusterPertenencia = asignarMuseosAClusters(MuseosFiltrados, clusters, nClusters);

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
        if (clusters.size() != ClusterNuevoBaricentro.size()) {
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
~~~
## Paralelización <a name="Algo4"></a>
Despues de correr el código de Clusterización nos dimos cuenta que converge muy rápido, por lo que una paralelización tendría un mínimo impacto sobre el tiempo de ejecución del algoritmo, no así con la Busqueda completa y al ser un algoritmo recursivo la paralelización se logra de manera muy rápida. de Modo que solo se hicieron algunos cambios, chequese
~~~java
GrafoNucleo auxCluster = new GrafoNucleo(auxMuseosRestantes, auxInicial, auxRuta);
auxCluster.start();
auxClusters1[i] = auxCluster;
~~~
Fue cambiado por:
~~~java
GrafoSecuencial auxCluster = new GrafoSecuencial(auxMuseosRestantes, auxInicial, auxRuta);
auxCluster.BusquedaCompleta();
auxClusters2[i] = auxCluster;
~~~
Donde GrafoNucleo es una clase que hereda los atributos de la clase Thread de Java y GrafoSecuencial es la versión secuencial del algoritmo.
## Tiempo secuencial vs Paralelo <a name="Algo5"></a>
Los siguientes _pantallazos_ muestran la cantidad de clusters generados, teniendo estos un límite de 11 museos (a lo más), el tiempo que tardó la versión paralela vs la secuencial. Adicionalmente añado la ganacia de eficiencia (tiempo secuencial/tiempo paralelo) y la cantidad de museos que se visitaron.

![image](https://github.com/JeroJAvenPa/BestWayBetweenMuseums/assets/111100048/73e6d119-3933-4667-bd36-a9fb55b91c63) \
Ganacia: $310.56\%$ con 93 museos visitados.

![image](https://github.com/JeroJAvenPa/BestWayBetweenMuseums/assets/111100048/7c65a6af-9b23-4d2f-beb5-f809bbcbacb9) \
Ganancia: $395.57\%$ con 93 museos visitados.

![image](https://github.com/JeroJAvenPa/BestWayBetweenMuseums/assets/111100048/7e3918b0-872c-455b-8b74-274243fa2a56) \
Ganancia: $418.45\%$ con 23 museos visitados.

![image](https://github.com/JeroJAvenPa/BestWayBetweenMuseums/assets/111100048/0470efee-de93-4db5-82c9-e3346714085e) \
Ganancia: $408.13\%$ con 182 museos visitados.

## Descarga del Repositorio <a name="Algo6"></a>
Puede ser descargado en su totalidad o con los archivos "App.Java", "prueba.html", "pom.xml" y "museos.csv", recomendamos que sea en su totalidad para accesibilidad del usuario.

### Compilar <a name="Algo6.1"></a>
Se debe de tener instalado en las variables de sistema mvn o en su defecto, descargar la carpeta en la ubicación de ...\maven\bin. Si cumple con los requisitos puede ejecutar sin problemas
``mvn compile``

### Ejecutar <a name="Algo6.2"></a>
Análogamente, si tiene instalado maven, ejecute
``mvn exec:java``
