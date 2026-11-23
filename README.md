# RPG Battle Gauntlet

Proyecto individual de segundo de DAW para el módulo de DWES. El objetivo es crear un RPG medieval para navegador en el que un equipo de tres héroes supere diez combates por turnos, con un dragón como jefe final.

## Fase 1 · Preparación del proyecto

La base usa Java 21, Spring Boot 3.5.16, Spring MVC y JUnit 5. El Maven Wrapper fija Maven 3.9.12 y descarga esa versión en la primera ejecución. Para descargar Maven y las dependencias hace falta conexión a Internet. La lógica se ejecutará en Java. El cliente HTML, CSS y JavaScript se comunicará con el servidor mediante una API REST y las partidas se almacenarán inicialmente en memoria.

## Compilar y ejecutar

Con un JDK 21 instalado, ejecuta desde la raíz:

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

En Windows, usa `mvnw.cmd` con los mismos argumentos. El servidor arranca en el puerto 8080 por defecto. Las pruebas JUnit comprueban el arranque del servidor, las estadísticas iniciales de las cinco clases, la validación de equipos y las reglas del motor de combate. Abre **http://localhost:8080** para jugar. Pulsa **Ctrl+C** para detener el servidor. Si el puerto está ocupado, puedes pasar otro con `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` y abrir el puerto elegido.

## Arquitectura

La clase `AplicacionGauntlet` está en el paquete raíz `es.rpgbattlegauntlet`. Cada subpaquete incluye un `package-info.java` con su responsabilidad.

| Paquete | Responsabilidad |
| --- | --- |
| `controller` | Gestionar las peticiones HTTP. |
| `model` | Representar partidas, personajes, enemigos y objetos. |
| `enums` | Definir valores cerrados, como estados y clases. |
| `service` | Coordinar las operaciones de la partida y validar sus reglas. |
| `algorithm` | Calcular daño, probabilidades y turnos, y decidir las acciones enemigas. |
| `dto` | Representar las entradas y salidas de la API. |
| `exception` | Representar los errores del juego y centralizar sus respuestas HTTP. |
| `config` | Centralizar los catálogos y parámetros de equilibrio. |

## Fase 2 · Héroes y selección de equipo

Cada partida empieza en `SELECCION_PERSONAJES` con el equipo vacío. La selección recibe exactamente tres clases distintas y conserva el orden elegido. Se puede cambiar el equipo durante esta etapa. Repetir la misma selección en el mismo orden conserva los identificadores de los héroes. La partida permanece en selección hasta que se inicia un combate.

| Clase | PV | PM | FUE | DEF | INT | RES | VEL | Crítico | Esquiva |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Guerrero | 150 | 20 | 15 | 15 | 3 | 8 | 6 | 5 % | 5 % |
| Mago | 75 | 100 | 4 | 5 | 18 | 14 | 9 | 5 % | 5 % |
| Arquero | 100 | 30 | 13 | 7 | 5 | 8 | 14 | 15 % | 10 % |
| Sanador | 90 | 90 | 5 | 8 | 15 | 13 | 8 | 5 % | 5 % |
| Pícaro | 85 | 25 | 12 | 6 | 4 | 7 | 17 | 20 % | 15 % |

Los héroes empiezan con los PV y PM completos. El catálogo centraliza los atributos base, que servirán para comprobar requisitos. Las estadísticas base, los modificadores de equipo y los estados temporales se mantienen por separado.

## Fase 3 · Combate por turnos

Con el equipo completo, la partida puede iniciar un combate local en Java y pasa a `COMBATE`. Derrotar al último rival lleva a `ENTRE_COMBATES`. Si los tres héroes caen, la partida pasa a `DERROTA`. Las pruebas del motor pueden proporcionar rivales genéricos y ejecutar ambos bandos directamente. Los enemigos del catálogo actúan automáticamente.

Cada ciclo ordena a los participantes vivos por velocidad efectiva descendente. Los empates conservan el orden del equipo y después el de los enemigos. La velocidad efectiva mínima es 1. Un cambio de velocidad influye al ordenar el siguiente ciclo. Los derrotados se retiran de la cola inmediatamente. El número de turno junto con el identificador del actor permite rechazar acciones repetidas o desfasadas.

### Fórmulas

- Daño físico bruto = `2 × (FUE base + FUE de equipo) + potencia de la acción`. Se resta la DEF efectiva del objetivo. Flecha perforante ignora el 50 % de esa defensa.
- Daño mágico bruto = `INT base + INT de equipo + potencia mágica de equipo + potencia de la acción`. Las habilidades mágicas de héroe tienen un límite de 64 antes de restar la RES. El límite reserva una potencia base mayor para los pergaminos.
- Después de restar DEF o RES, se conserva al menos el 25 % del daño bruto. Un crítico multiplica el valor por 1,5. A continuación se aplica el mayor porcentaje activo de reducción de daño, se redondea al entero más cercano y se aplica un mínimo de 1 si el golpe alcanza al objetivo.
- La probabilidad de esquiva efectiva incorpora los estados de esquiva y incluye el equipo y se reduce según la precisión adicional del ataque. El crítico suma los aportes de equipo y habilidad a la probabilidad base. Se tira primero la esquiva y, si el ataque acierta, se tira el crítico. Los ataques de área realizan esas tiradas por separado para cada objetivo.
- La curación equivale a `potencia de la habilidad + 2 × (INT base + INT de equipo) + potencia mágica de equipo`, con un límite de 89 y sin superar los PV máximos del aliado vivo. La curación puede apuntar al propio sanador.
- Los estados temporales duran hasta que el objetivo completa el número indicado de acciones posteriores a su aplicación. Volver a aplicar un estado conserva la mayor duración restante. Las reducciones de daño simultáneas usan el porcentaje mayor.

### Habilidades disponibles

| Clase | Habilidad y coste | Habilidad y coste |
| --- | --- | --- |
| Guerrero | Golpe poderoso · 5 PM | Guardia · 4 PM |
| Mago | Bola de fuego · 9 PM | Descarga arcana · 13 PM |
| Arquero | Disparo preciso · 4 PM | Flecha perforante · 6 PM |
| Sanador | Curación · 8 PM | Bendición · 6 PM |
| Pícaro | Ataque furtivo · 5 PM | Esquivar · 4 PM |

El ataque básico y Defenderse son gratuitos y consumen una acción. Defenderse reduce el daño un 40 % durante un turno. Guardia lo reduce un 55 % durante dos turnos. Bendición aplica un 25 % a un aliado durante dos turnos. Esquivar concede 50 puntos porcentuales de esquiva durante un turno. Flecha perforante resta 4 de velocidad durante un turno al alcanzar al objetivo. Disparo preciso resta hasta 25 puntos porcentuales de esquiva en su tirada. Ataque furtivo añade 20 puntos porcentuales de crítico.

El historial registra una entrada por objetivo afectado, con daño o curación efectiva, esquiva y crítico. Los consumibles que recuperan PM también registran la magia recuperada.

## Fase 4 · Enemigos e IA

`CatalogoEnemigos` centraliza las estadísticas iniciales y las habilidades de las seis especies. Cada creación devuelve un enemigo con identidad propia y sus PV y PM completos. Los enemigos del catálogo actúan automáticamente al comenzar el combate y después de cada acción del jugador, hasta que vuelve a tocarle a un héroe o se acaba el combate. En los ataques individuales priorizan al héroe vivo con menos PV actuales. En caso de empate se conserva el orden del equipo.

| Enemigo | Comportamiento |
| --- | --- |
| Goblin | Ataca con la daga. Realiza dos golpes independientes en la segunda acción de cada bloque de tres. Si el primero derrota al objetivo, el segundo se omite. |
| Esqueleto | Usa Espadazo y Guardia ósea. La guardia reduce el daño un 45 % durante un turno. La usa en la segunda acción de cada bloque de tres y también en la primera si tiene el 35 % de sus PV o menos. |
| Chamán | Si puede gastar 14 PM, cura al aliado vivo con la menor proporción de PV entre quienes tienen como máximo el 70 %. Puede curarse a sí mismo. Si no cura, usa Bola de fuego por 10 PM o el ataque básico si le falta magia. |
| Ogro | Alterna Mazazo con un barrido físico. Si queda un solo héroe, usa Mazazo. |
| Caballero oscuro | Usa Espada maldita, que resta 5 DEF durante dos turnos al acertar. En la segunda acción de cada bloque de cuatro usa Guardia oscura por 8 PM y reduce el daño un 55 % durante un turno. En la tercera puede gastar 6 PM para preparar un contraataque. Si sobrevive al siguiente golpe, reacciona una vez y registra esa reacción en el turno del atacante. |
| Dragón | Por encima del 70 % de PV usa Garra. Entre el 35 % exclusivo y el 70 % alterna Garra y Aliento de fuego. Con el 35 % o menos usa Rugido en la segunda acción de cada bloque de tres y prioriza Aliento en las otras dos. Aliento cuesta 20 PM y causa daño mágico de área. Rugido cuesta 8 PM y ralentiza a todos los héroes vivos durante un turno. Si le falta PM para la acción prevista, usa Garra. |

Los ciclos y umbrales son decisiones de esta fase. Los encuentros se definen en un catálogo independiente.

## Fase 5 · Encuentros fijos

| Ronda | Enemigos |
| ---: | --- |
| 1 | Goblin |
| 2 | Dos goblins |
| 3 | Goblin y esqueleto |
| 4 | Esqueleto y chamán |
| 5 | Ogro |
| 6 | Dos goblins y chamán |
| 7 | Caballero oscuro |
| 8 | Caballero oscuro y chamán |
| 9 | Dos caballeros oscuros |
| 10 | Dragón |

Tras seleccionar el equipo, `iniciarCombate()` abre la ronda 1. En las rondas 1 a 9, ganar lleva a `ENTRE_COMBATES`. Para abrir la siguiente ronda se llama a `avanzarRonda(numeroDeRondaCompletada)` con el número de la ronda recién ganada. Derrotar al dragón lleva directamente a `VICTORIA`, sin parada de preparación ni recompensas. Perder con los tres héroes lleva a `DERROTA`.

Cada ronda crea enemigos nuevos con sus estadísticas de catálogo. Los héroes conservan su identidad, PV y PM entre rondas. El avance conserva los recursos del héroe, incluida la recuperación que haya recibido por subir de nivel. Los caídos permanecen fuera de combate y los estados temporales se limpian al finalizar cada encuentro. El número de turno continúa a lo largo de la partida para rechazar acciones atrasadas de rondas anteriores, y el historial de la partida conserva las acciones de las rondas anteriores.

## Fase 6 · Inventario y equipamiento

`CatalogoObjetos` describe el catálogo fijo y los objetos iniciales. Cada unidad adquirida o equipada tiene su propio UUID. Durante la pausa entre combates, se puede intercambiar equipo entre los héroes a través del inventario común. El objeto sustituido vuelve al inventario. Los objetos iniciales se pueden quitar, conservar y reasignar. La tienda rechaza su venta. El inventario no tiene límite de espacio.

Cada héroe tiene una ranura de arma y otra de armadura. Capuchas, túnicas, botas, cascos y escudos usan la ranura de armadura. La FUE y la INT **base** determinan si se puede equipar una pieza, independientemente de la clase y la calidad. Las túnicas añaden potencia mágica al daño y a la curación. Las armaduras pesadas aplican su penalización a la velocidad efectiva. Las habilidades de clase siguen disponibles al cambiar de arma.

| Clase | Arma inicial | Armadura inicial |
| --- | --- | --- |
| Guerrero | Espada básica | Armadura pesada básica |
| Mago | Vara de aprendiz | Túnica de aprendiz |
| Arquero | Arco corto | Armadura ligera básica |
| Sanador | Vara de aprendiz | Túnica de aprendiz |
| Pícaro | Daga | Ropa ligera |

| Familia | Habilidad de arma | Coste y efecto adicional |
| --- | --- | --- |
| Daga | Estocada rápida | 3 PM, potencia física +5, precisión +20 y crítico +5 puntos porcentuales |
| Espada | Corte amplio | 4 PM, potencia física +10 y precisión +10 puntos porcentuales |
| Arco | Disparo tenso | 5 PM, potencia física +10, precisión +15 puntos porcentuales y 25 % de penetración |
| Vara | Pulso de vara | 5 PM, potencia mágica +13 y precisión +10 puntos porcentuales |
| Garrote | Impacto contundente | 5 PM, potencia física +14 y 15 % de penetración |

## Fase 7 · Botín y calidades

Cada enemigo vencido tiene una tirada independiente para soltar como máximo un objeto. Si la supera, una segunda tirada elige el objeto según los pesos condicionados de la tabla. El botín se recoge por unidad en Recompensas. Si se avanza con unidades pendientes, se deja atrás ese botín.

| Enemigo | Probabilidad de soltar | Reparto si suelta |
| --- | ---: | --- |
| Goblin | 35 % | Daga 45 %, capucha 30 %, armadura ligera 25 % |
| Esqueleto | 40 % | Espada 40 %, escudo 35 %, armadura antigua 25 % |
| Chamán | 50 % | Vara mágica 45 %, túnica de mago 35 %, pergamino 20 % |
| Ogro | 65 % | Garrote 50 %, armadura pesada 30 %, botas reforzadas 20 % |
| Caballero oscuro | 75 % | Espada maldita 40 %, armadura oscura 35 %, casco oscuro 25 % |
| Dragón | 0 % | — |

| Calidad | Probabilidad | Multiplicador | Venta sobre el valor base | Recompra sobre el valor base |
| --- | ---: | ---: | ---: | ---: |
| Inferior | 35 % | 0,75 | 30 % | 60 % |
| Normal | 50 % | 1,00 | 50 % | 100 % |
| Superior | 15 % | 1,25 | 75 % | 150 % |

Las bonificaciones se redondean al entero más próximo por magnitud y conservan su signo. Cada calidad tiene un nombre propio. Los requisitos se mantienen y los precios se calculan directamente a partir del valor base. El documento fijaba el precio de varios objetos especiales sin definir sus atributos. Para ellos se eligieron los siguientes valores.

| Objeto especial | Bonificaciones normales | Requisito |
| --- | --- | --- |
| Espada del esqueleto | +6 FUE | 12 FUE |
| Escudo | +6 DEF | 10 FUE |
| Armadura antigua | +7 DEF, +2 RES, −1 VEL | 12 FUE |
| Espada maldita | +14 FUE | 20 FUE |
| Casco oscuro | +10 DEF, +5 RES | 16 FUE |

## Fase 8 · Tienda y economía

La tienda se abre entre combates. Tiene cuatro apartados, **Armas**, **Armaduras**, **Pociones** y **Pergaminos**. Las armas, armaduras y pociones del catálogo habitual son ilimitadas. Cada hechizo tiene una unidad normal por partida. Una vez comprada, aparece como agotada aunque luego se venda o consuma el objeto. Los artículos vendidos aparecen en el mismo apartado como segunda mano. Recomprar retira exactamente esa unidad y conserva su UUID, variante, estadísticas y precio de recompra.

La partida empieza con 0 MO. Una poción se vende por la mitad del precio habitual redondeada al alza y se recompra al precio habitual. Las reglas de venta y recompra de equipo están en `Calidad`. Los pergaminos débiles, normales y poderosos se venden por el 30 %, 50 % y 80 % del precio normal redondeado al entero más cercano. La recompra cuesta el doble de la venta. Estas reglas viven en `PotenciaPergamino`, con el precio de cada hechizo en `CatalogoObjetos`. Por ejemplo, Meteorito poderoso se vende por 200 MO y se recompra por 400 MO.

## Fase 9 · Pociones y pergaminos

| Poción | Recuperación | Precio |
| --- | --- | ---: |
| Roja | 80 PV | 40 MO |
| Verde | 50 PM | 40 MO |
| Azul | 50 PV y 30 PM | 55 MO |

Las pociones se pueden usar en combate y durante la preparación. En combate, quien la usa gasta su acción y puede elegir un aliado vivo, incluido él mismo. En preparación, se aplica al héroe elegido. La recuperación se limita a los PV y PM máximos. Un consumo que falla la validación conserva el objeto.

Los pergaminos se usan en combate, requieren la INT base indicada y gastan una acción sin consumir PM. Los ofensivos tienen potencia fija, aciertan sin tirada, no hacen crítico y se reducen por RES y protección. Los de área se aplican a los enemigos vivos y las curaciones a los aliados vivos. Si el botín del chamán es un pergamino, se elige el hechizo con probabilidad uniforme del 10 %. Después se sortea la potencia con probabilidades de 35 %, 50 % y 15 % para débil, normal y poderoso. La INT exigida es la de la tabla menos 2 en débil y más 3 en poderoso.

| Pergamino | Efecto débil / normal / poderoso | INT normal | Precio fijo |
| --- | --- | ---: | ---: |
| Proyectil mágico | 50 / 65 / 90 de daño individual | 5 | 65 MO |
| Cura | 65 / 90 / 130 PV a un aliado | 6 | 75 MO |
| Cura mayor | 90 / 120 / 160 PV a un aliado | 8 | 100 MO |
| Llamarada | 75 / 100 / 140 de daño individual | 10 | 110 MO |
| Ventisca | 45 / 65 / 90 de daño de área | 12 | 140 MO |
| Cura grupal | 50 / 70 / 100 PV por aliado vivo | 14 | 150 MO |
| Tormenta eléctrica | 60 / 80 / 115 de daño de área | 15 | 170 MO |
| Protección divina | 35 / 50 / 65 % de reducción de daño para el equipo durante dos turnos | 16 | 160 MO |
| Cura total | 75 / 100 / 100 % de los PV máximos a un aliado vivo. Poderoso también aplica un 35 % de protección durante dos turnos | 18 | 180 MO |
| Meteorito | 100 / 140 / 200 de daño de área | 22 | 250 MO |

## Fase 10 · Experiencia, niveles y recompensas

En cada victoria de las rondas 1 a 9 se ingresan automáticamente el oro y la experiencia. Los tres héroes reciben la misma experiencia, incluidos los caídos, y el botín queda disponible para recoger. Cada ronda liquida sus recompensas una sola vez. La ronda 10 otorga la victoria inmediatamente y conserva el oro, la experiencia y el inventario sin añadir nuevas recompensas.

- Experiencia por ronda = `75 + 18 × (ronda − 1)`.
- Experiencia para subir = `75 + 35 × (nivel actual − 1)`. El sobrante pasa al siguiente nivel.
- Oro = el mayor valor entre `60 + 13 × ronda` y `95 + 14 × ronda + 12 × supervivientes − 2 × acciones − 6 × consumibles`. Se cuenta cada acción de héroe o enemigo como un turno. Los golpes múltiples y contraataques no añaden turnos, y solo se cuentan los consumibles usados durante ese combate.
- Cada nivel concede tres puntos. Un punto de PV añade 12 PV máximos y uno de PM añade 8 PM máximos. El resto de atributos gana una unidad base por punto. Invertir en PV o PM también suma ese incremento al recurso actual de un héroe vivo.
- Por cada nivel, un héroe vivo recupera hasta 30 PV y 20 PM. Un héroe caído mantiene 0 PV tras subir de nivel o invertir puntos.

## Fase 11 · API REST

Spring MVC sirve la API y, desde el mismo origen, los archivos estáticos. `GestorPartidas` guarda las partidas en memoria y coordina las operaciones. Reiniciar la aplicación borra el progreso de todas las partidas. El navegador solo conserva el identificador de la partida activa.

| Método y ruta | Uso |
| --- | --- |
| `GET /api/partidas/clases` | Clases y estadísticas base |
| `POST /api/partidas` | Crea una partida y devuelve `201 Created` |
| `GET /api/partidas/{id}` | Estado, turnos, recursos, equipo, inventario, botín y recompensas |
| `GET /api/partidas/{id}/historial` | Historial de todas las rondas jugadas |
| `POST /api/partidas/{id}/{operación}` | Ejecuta una operación y devuelve la vista actualizada |

Envía la `version` de la última vista con cada operación. El servidor valida esa versión bajo un bloqueo por partida. Si se repite una petición válida con la versión anterior, responde `409` sin repetir el cambio. Una operación fuera de estado también devuelve `409`. Las peticiones inválidas reciben `400` y las partidas inexistentes, `404`. Las respuestas de error incluyen `mensaje` para la interfaz.

| Operación | Campos JSON además de `version` |
| --- | --- |
| `seleccion` | `clases`, lista de tres códigos distintos |
| `iniciar` | Ninguno |
| `accion` | `turno`, `actor`, `habilidad` y `objetivo` cuando sea individual |
| `avanzar` | `ronda`, con la ronda recién ganada |
| `recoger` | `objeto`, UUID del botín |
| `equipar` | `actor`, `objeto` |
| `desequipar` | `actor`, `categoria` (`ARMAS` o `ARMADURAS`) |
| `comprar` | `codigo` de catálogo o `objeto` de segunda mano |
| `vender` | `objeto` |
| `consumir` | `actor`, `objeto`, `turno` en combate, `objetivo` cuando sea individual |
| `atributos` | `actor`, `atributo`, `cantidad` |
| `reiniciar` | Ninguno. Cierra el identificador anterior y devuelve otra partida. |

Ejemplo de cuerpo para seleccionar el equipo tras crear una partida con versión 0.

```json
{"version":0,"clases":["ARQUERO","SANADOR","GUERRERO"]}
```

## Fase 12 · Interfaz web

La portada permite crear una partida y seleccionar tres clases. En combate se muestran los dos bandos, barras de PV y PM, actor actual, orden pendiente, estados, acciones con objetivos válidos e historial. El campamento contiene Recompensas, Inventario, Equipamiento, Tienda y Nivel. La tienda compara con la ranura equipada del héroe elegido. Victoria y derrota permiten reiniciar. Al recargar, el navegador recupera el progreso si el servidor sigue ejecutándose.

Los archivos `index.html`, `estilos.css` y `juego.js` se sirven desde `src/main/resources/static`. Se usan símbolos provisionales y estilos locales sin imágenes externas ni frameworks. El cliente usa `fetch` para obtener el estado y enviar las decisiones del jugador. El backend calcula todos los resultados y valida las operaciones.

## Fase 13 · Pruebas y equilibrio

Ejecuta `./mvnw clean verify` para compilar, probar y empaquetar el JAR en `target/`. Puedes arrancar el resultado directamente con `java -jar target/rpg-battle-gauntlet-0.1.0-SNAPSHOT.jar`.

JUnit cubre la selección, las estadísticas, los turnos, las fórmulas, la IA, la progresión por las diez rondas, las recompensas, el inventario, los requisitos, el botín controlado, el consumo, la tienda, las operaciones HTTP y los reintentos simultáneos. Las pruebas de reglas inyectan el azar para controlar los resultados. La prueba de progresión debilita los enemigos deliberadamente para comprobar el flujo de estados. Las simulaciones de equilibrio usan combates reales, tienda y atributos, sin recoger botín.

`CampanaTest` reproduce 30 partidas con semillas 0 a 29 y un equipo de arquero, sanador y guerrero. La estrategia mejora el arma del arquero y del guerrero, invierte en PV y usa pociones, curación y pergaminos de tienda para el tramo final. Ignora todo el botín. Las 30 ejecuciones prueban que esa estrategia puede completar la campaña con las reglas y recursos de tienda. Probar otras composiciones y estrategias permitiría medir un equilibrio más amplio.

También se verificó el JAR en Chrome. El recorrido creó una partida, eligió héroes, ganó el primer combate, visitó los cuatro apartados de la tienda, compró una poción y asignó un punto de atributo. Después recargó la página, comprobó el diseño a 360 px, avanzó a la ronda 2 y reinició la partida. El resultado de la compilación final fue de 71 pruebas sin fallos ni errores.
