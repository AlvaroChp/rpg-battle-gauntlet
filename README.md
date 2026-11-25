# RPG Battle Gauntlet

Proyecto individual de segundo de DAW para el módulo de DWES. El objetivo es crear un RPG medieval para navegador en el que un equipo de tres héroes supere diez combates por turnos, o quince en Experto y Pruebas, con un dragón como jefe final.

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

| Clase | PV | PC | FUE | DEF | INT | RES | VEL | DES | Esquiva |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Guerrero | 150 | 20 | 15 | 15 | 3 | 8 | 6 | 10 | 5 % |
| Mago | 75 | 100 | 4 | 5 | 18 | 14 | 9 | 3 | 5 % |
| Arquero | 100 | 30 | 13 | 7 | 5 | 8 | 14 | 15 | 10 % |
| Sanador | 90 | 90 | 5 | 8 | 15 | 13 | 8 | 3 | 5 % |
| Pícaro | 85 | 25 | 12 | 6 | 4 | 7 | 17 | 25 | 15 % |

Los héroes empiezan con los puntos de vida (PV) y los puntos de concentración (PC) completos. El catálogo centraliza los atributos base, que servirán para comprobar requisitos. Las estadísticas base, los modificadores de equipo y los estados temporales se mantienen por separado.

## Fase 3 · Combate por turnos

Con el equipo completo, la partida puede iniciar un combate local en Java y pasa a `COMBATE`. Derrotar al último rival lleva a `ENTRE_COMBATES`. Si los tres héroes caen, la partida pasa a `DERROTA`. Las pruebas del motor pueden proporcionar rivales genéricos y ejecutar ambos bandos directamente. Los enemigos del catálogo actúan automáticamente.

Cada ciclo ordena a los participantes vivos por velocidad efectiva descendente. Los empates conservan el orden del equipo y después el de los enemigos. La velocidad efectiva mínima es 1. Un cambio de velocidad influye al ordenar el siguiente ciclo. Los derrotados se retiran de la cola inmediatamente. El número de turno junto con el identificador del actor permite rechazar acciones repetidas o desfasadas.

### Fórmulas

- Daño físico bruto = `2 × (FUE base + FUE de equipo) + potencia de la acción`. Se resta la DEF efectiva del objetivo. Flecha perforante ignora el 50 % de esa defensa.
- Daño mágico bruto = `INT base + INT de equipo + potencia mágica de equipo + potencia de la acción`. Las habilidades mágicas de héroe tienen un límite de 64 antes de restar la RES. El límite reserva una potencia base mayor para los pergaminos.
- Después de restar DEF o RES, se conserva al menos el 25 % del daño bruto. Un crítico multiplica el valor por 1,5. A continuación se aplica el mayor porcentaje activo de reducción de daño, se redondea al entero más cercano y se aplica un mínimo de 1 si el golpe alcanza al objetivo.
- Los ataques mágicos aciertan siempre, no hacen crítico y se reducen por la resistencia y la protección del objetivo. En los ataques físicos, la esquiva efectiva incorpora los estados y el equipo, y se reduce según la precisión adicional del ataque. Cada punto de destreza aporta un punto porcentual de crítico base para los héroes. Los enemigos usan su probabilidad base propia. A la base se suman los aportes de equipo y habilidad. Se tira primero la esquiva y, si el ataque acierta, se tira el crítico. Los ataques físicos de área realizan esas tiradas por separado para cada objetivo.
- La curación equivale a `potencia de la habilidad + 2 × (INT base + INT de equipo) + potencia mágica de equipo`, con un límite de 89 y sin superar los PV máximos del aliado vivo. La curación puede apuntar al propio sanador.
- Los estados temporales duran hasta que el objetivo completa el número indicado de acciones posteriores a su aplicación. Volver a aplicar un estado conserva la mayor duración restante. Las reducciones de daño simultáneas usan el porcentaje mayor.
- Flecha envenenada y los ataques de fuego aplican su estado con un 50 % de probabilidad, o un 25 % si el objetivo tiene Defenderse o Bendición. Incendio de ceniza usa un 100 % o un 50 %, respectivamente. El veneno inflige 4 de daño y la quemadura inflige 5 al final de cada uno de los dos próximos turnos del objetivo. Volver a aplicar el mismo estado renueva su duración sin acumular daño.

### Habilidades disponibles

| Clase | Habilidad y coste | Habilidad y coste |
| --- | --- | --- |
| Guerrero | Golpe poderoso · 5 PC | Interceptar · 4 PC |
| Mago | Bola de fuego · 9 PC | Descarga arcana · 13 PC |
| Arquero | Disparo preciso · 4 PC | Flecha perforante · 6 PC |
| Sanador | Curación · 8 PC | Bendición · 6 PC |
| Pícaro | Robar · 5 PC | Esquivar · 4 PC |

El ataque básico y Defenderse son gratuitos y consumen una acción. Defenderse reduce el daño un 40 % durante un turno, o un 65 % con un escudo equipado. El escudo también eleva al 65 % la reducción del golpe cubierto por Interceptar. Interceptar protege el primer golpe que alcance al guerrero antes de su próximo turno. Si el enemigo apunta a un aliado con un ataque individual, el guerrero lo recibe en su lugar. El impacto usa la reducción de Defenderse y su protección contra quemaduras y veneno. En ataques de área, cada aliado recibe su propio golpe. Bendición aplica un 25 % a un aliado durante dos turnos. Esquivar concede 50 puntos porcentuales de esquiva durante un turno. Flecha perforante resta 4 de velocidad durante un turno al alcanzar al objetivo. Disparo preciso resta hasta 25 puntos porcentuales de esquiva en su tirada. Robar tiene una probabilidad de éxito del 20 % más dos puntos porcentuales por punto de destreza, con un tope del 90 %. Un robo exitoso coloca el objeto en el inventario y agota el botín de ese enemigo. Un intento fallido permite reintentar. El dragón puede ser objetivo de Robar y lleva el Pergamino de Dragón. El pergamino requiere 22 INT base y derrota al dragón de un solo golpe.

El historial registra una entrada por objetivo afectado, con daño o curación efectiva, esquiva y crítico. Los consumibles que recuperan PC también registran la concentración recuperada.

## Fase 4 · Enemigos e IA

`CatalogoEnemigos` centraliza las estadísticas iniciales y las habilidades de las siete especies. Cada creación devuelve un enemigo con identidad propia y sus PV y PC completos. Los enemigos del catálogo actúan automáticamente al comenzar el combate y después de cada acción del jugador, hasta que vuelve a tocarle a un héroe o se acaba el combate. En el 75 % de los ataques individuales el objetivo se sortea entre todos los héroes vivos. En el 25 % restante se sortea entre quienes tienen el menor porcentaje de PV.

| Enemigo | Comportamiento |
| --- | --- |
| Goblin | Empieza con 12 PC. Gasta 4 PC en Ataque doble en la segunda acción de cada bloque de tres. Si el primer golpe derrota al objetivo, el segundo se omite. En sus demás acciones usa Cuchillada. |
| Goblin arquero | Empieza con 16 PC. Alterna Flechazo goblin con Flecha envenenada del Arco goblin mientras pueda pagar sus 4 PC. |
| Esqueleto | Empieza con 16 PC. Guardia ósea cuesta 4 PC y reduce el daño un 45 % durante un turno. Intenta usarla en la segunda acción de cada bloque de tres y también en la primera si tiene el 35 % de sus PV o menos. Si no puede usarla, recurre a Espadazo. |
| Chamán | Si puede gastar 14 PC, cura al aliado vivo con la menor proporción de PV entre quienes tienen como máximo el 70 %. Puede curarse a sí mismo. Si no cura, usa Bola de fuego por 10 PC o el ataque básico si le falta concentración. |
| Ogro | Empieza con 24 PC. Alterna Mazazo con un Barrido físico que cuesta 6 PC. Si queda un solo héroe o le falta concentración, usa Mazazo. |
| Caballero oscuro | Si tiene un compañero caído y conserva su poción de revitalización, gasta el turno en revivirlo con el 25 % de sus PV máximos. Un robo exitoso se lleva esa poción mientras la conserve. En sus demás acciones usa Espada maldita, que resta 5 DEF durante dos turnos, Guardia oscura por 8 PC en la segunda acción de cada bloque de cuatro, y prepara un contraataque por 6 PC en la tercera. |
| Dragón | Por encima del 70 % de PV usa Garra. Entre el 35 % exclusivo y el 70 % alterna Garra y Aliento de fuego. Con el 35 % o menos usa Rugido en la segunda acción de cada bloque de tres y prioriza Aliento en las otras dos. Aliento cuesta 20 PC y causa daño mágico de área. Rugido cuesta 8 PC y ralentiza a todos los héroes vivos durante un turno. Si le falta PC para la acción prevista, usa Garra. |

El ogro anuncia Furia animal al llegar al 50 % de PV o menos. En su siguiente turno lanza un ataque físico de área. Un crítico físico que le alcance durante la carga lo interrumpe. El dragón anuncia Incendio de ceniza al llegar al 35 % de PV o menos y en su siguiente turno lanza un ataque mágico de área que puede quemar. Cada preparación ocurre una vez por combate.

En su tercer turno de cada bloque de tres, los enemigos que pueden soltar armas pueden usar un ataque de esas armas en lugar de su ataque individual habitual. El caballero oscuro lo hace en el cuarto turno de su ciclo y el goblin arquero, en los turnos pares. La IA sortea entre las armas cuyo ataque pueda pagar con sus PC. Los ataques cuestan lo mismo que a los héroes y las acciones de curación, defensa o área previstas conservan la prioridad.

Los encuentros se definen en un catálogo independiente.

### Perfiles para la ampliación de Experto

Estas estadísticas base son el punto de partida para probar los nuevos encuentros. Se aplican los modificadores de dificultad habituales.

| Enemigo | PV | PC | FUE | DEF | INT | RES | VEL | Crítico | Esquiva |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Nigromante | 125 | 58 | 6 | 8 | 19 | 15 | 8 | 5 % | 5 % |
| Gólem rúnico | 280 | 0 | 20 | 27 | 0 | 6 | 4 | 5 % | 0 % |
| Verdugo espectral | 185 | 0 | 20 | 13 | 0 | 12 | 9 | 8 % | 8 % |

- **Nigromante.** Lanza Descarga fúnebre por 10 PC. Si cae un aliado, prioriza Reanimar por 18 PC y le devuelve el 25 % de los PV máximos. Puede hacerlo una vez por combate si conserva magia suficiente.
- **Gólem rúnico.** Anuncia Onda rúnica, golpea al grupo en su siguiente acción y después usa Puño de piedra antes de volver a cargar. Tiene una DEF alta y una RES baja.
- **Verdugo espectral.** Abre con Tajo espectral, marca a un héroe y dirige Sentencia espectral a ese mismo objetivo en su siguiente acción. La marca aparece sobre el héroe. Si el verdugo o el objetivo caen antes del golpe, la marca se cancela. El golpe conserva las reglas habituales de defensa e Interceptar. En la cuarta acción de su ciclo usa Barrido espectral, un ataque físico contra todos los héroes vivos con −12 de potencia.

## Fase 5 · Encuentros fijos

| Ronda | Enemigos |
| ---: | --- |
| 1 | Goblin |
| 2 | Goblin y goblin arquero |
| 3 | Goblin y esqueleto |
| 4 | Esqueleto, chamán y goblin arquero |
| 5 | Ogro |
| 6 | Dos esqueletos y chamán |
| 7 | Caballero oscuro |
| 8 | Caballero oscuro y chamán |
| 9 | Dos caballeros oscuros |
| 10 | Dragón |

Experto y Pruebas comparten la campaña de quince rondas. Las rondas 1 a 9 son comunes a todos los modos y el tramo final ampliado tiene seis encuentros.

| Ronda | Enemigos en Experto y Pruebas |
| ---: | --- |
| 10 | Verdugo espectral en solitario (minijefe) |
| 11 | Nigromante y caballero oscuro |
| 12 | Gólem rúnico en solitario |
| 13 | Dos nigromantes |
| 14 | Dos caballeros oscuros y nigromante |
| 15 | Dragón |

Tras seleccionar el equipo, `iniciarCombate()` abre la ronda 1. Ganar cualquier ronda anterior al dragón lleva a `ENTRE_COMBATES`. Para abrir la siguiente ronda se llama a `avanzarRonda(numeroDeRondaCompletada)` con el número de la ronda recién ganada. Derrotar al dragón lleva directamente a `VICTORIA`, sin parada de preparación ni recompensas. Perder con los tres héroes lleva a `DERROTA`.

Cada ronda crea enemigos nuevos con sus estadísticas de catálogo. Los héroes conservan su identidad, PV y PC entre rondas. El avance conserva los recursos del héroe, incluida la recuperación que haya recibido por subir de nivel. Los caídos permanecen fuera de combate y los estados temporales se limpian al finalizar cada encuentro. El número de turno continúa a lo largo de la partida para rechazar acciones atrasadas de rondas anteriores, y el historial de la partida conserva las acciones de las rondas anteriores.

## Fase 6 · Inventario y equipamiento

`CatalogoObjetos` describe el catálogo fijo y los objetos iniciales. Cada unidad adquirida o equipada tiene su propio UUID. Durante la pausa entre combates, se puede intercambiar equipo entre los héroes a través del inventario común. El objeto sustituido vuelve al inventario. Las armas equipadas solo se pueden sustituir por otra arma compatible. Las armaduras se pueden desequipar. Las armas y armaduras iniciales se pueden vender desde el inventario. El inventario no tiene límite de espacio.

Cada héroe tiene seis ranuras para **arma, torso, guantes, botas, cabeza y escudo**. Las armaduras, túnicas y ropas van en torso. Las capuchas y yelmos van en cabeza. Las cinco ranuras de protección comparten el apartado **Armaduras** de la tienda. Sus bonificaciones se suman y las demás ranuras permanecen al cambiar una pieza. La FUE y la INT **base** determinan si se cumplen los requisitos de atributos de una pieza. Guerrero y pícaro pueden usar espadas, dagas y garrotes. Mago y sanadora pueden usar varas. La arquera puede usar arcos y ballestas. Las túnicas añaden potencia mágica al daño y a la curación. Las armaduras pesadas aplican su penalización a la velocidad efectiva. La armadura pesada de la tienda y del botín requiere 16 FUE base en todas sus calidades. La armadura pesada básica del guerrero requiere 15 FUE base. Las habilidades de clase siguen disponibles al cambiar de arma.

El escudo es exclusivo de guerrero y pícaro y exige tener equipada un arma de una mano. Arcos, ballestas, varas, garrotes, espadas pesadas y espadas malditas son de dos manos en todas sus calidades. Si se equipa una de ellas, el escudo y el arma anterior regresan al inventario. Los escudos siguen otorgando un 65 % de reducción con Defenderse o Interceptar.

Como ahora las piezas se acumulan, se redujeron los bonos pasivos de las piezas reutilizadas en cabeza, botas y escudo. El torso conserva sus estadísticas. Las dos clases de guantes se venden en Armaduras.

| Pieza | Bonificaciones normales | Precio o procedencia |
| --- | --- | --- |
| Capucha | +1 DEF y +2 puntos porcentuales de esquiva | 45 MO |
| Botas reforzadas | +1 DEF y +2 VEL | 130 MO o botín |
| Casco reforzado | +3 DEF y +1 RES | 190 MO |
| Casco oscuro | +3 DEF y +2 RES | Botín |
| Escudo | +2 DEF y mejora de Defenderse e Interceptar | Botín |
| Sombrero de mago | +1 RES y +2 de potencia mágica | 80 MO, 12 INT base |
| Guantes de cuero | +1 DEF y +1 RES | 45 MO, sin requisito |
| Guantes de malla | +2 DEF | 75 MO, 14 FUE base |

| Clase | Arma inicial | Armadura inicial |
| --- | --- | --- |
| Guerrero | Espada básica | Armadura pesada básica |
| Mago | Vara de aprendiz | Túnica de aprendiz |
| Arquero | Arco corto | Armadura ligera básica |
| Sanador | Vara de aprendiz | Túnica de aprendiz |
| Pícaro | Daga | Ropa ligera |

Cada tipo de arma concede su propio ataque. Sus tres calidades comparten el ataque y modifican las bonificaciones del arma.

| Arma | Ataque | Coste y efecto adicional |
| --- | --- | --- |
| Daga | Estocada rápida | 3 PC, potencia física +5, precisión +20 y crítico +5 puntos porcentuales |
| Daga de duelo | Puntada traicionera | 5 PC, potencia física +8, precisión +10 y crítico +20 puntos porcentuales |
| Daga curva | Corte al tendón | 4 PC, potencia física +7 y −4 VEL durante un turno si acierta |
| Daga ritual | Sangría ritual | 5 PC, potencia física +8 y recupera como PV la mitad del daño efectivamente infligido, redondeada hacia abajo |
| Espada básica | Corte amplio | 4 PC, potencia física +10 y precisión +10 puntos porcentuales |
| Espada de acero | Estocada precisa | 4 PC, potencia física +12 y precisión +30 puntos porcentuales |
| Espada pesada | Tajo demoledor | 6 PC, potencia física +16 y 30 % de penetración |
| Espada del esqueleto | Corte de ultratumba | 4 PC, potencia física +11 y ralentización durante un turno |
| Espada maldita | Filo de la maldición | 6 PC, potencia física +14 y −5 DEF durante dos turnos |
| Arco corto | Disparo tenso | 5 PC, potencia física +10, precisión +15 puntos porcentuales y 25 % de penetración |
| Arco goblin | Flecha envenenada | 4 PC, potencia física +4, precisión +10 puntos porcentuales y posibilidad de envenenar al acertar |
| Arco largo | Flecha de largo alcance | 5 PC, potencia física +12, precisión +15 puntos porcentuales y 30 % de penetración |
| Ballesta | Virote perforante | 6 PC, potencia física +12, precisión +10 puntos porcentuales y 35 % de penetración |
| Vara de aprendiz | Pulso de vara | 5 PC, potencia mágica +13 |
| Vara mágica | Destello arcano | 4 PC, potencia mágica +12 |
| Vara de chamán | Eco de la tormenta | 9 PC, potencia mágica +10 a todos los enemigos y −4 VEL durante un turno |
| Vara avanzada | Descarga astral | 8 PC, potencia mágica +22 |
| Garrote | Impacto contundente | 5 PC, potencia física +14 y 15 % de penetración |

La daga ritual tiene +5 FUE y +3 puntos porcentuales de crítico en calidad normal. Requiere 12 FUE base y la pueden equipar guerrero y pícaro. Se obtiene de los chamanes y los nigromantes. Ambos pueden llevarla como botín o perderla con Robar. Sangría ritual cuenta los PV que pierde el objetivo tras sus defensas, incluidos los críticos, y la curación se limita a los PV máximos del portador.

La Espada maldita concede **Furia maldita** al caballero oscuro y al guerrero o pícaro que la equipe. Por cada punto porcentual de PV perdido, añade 0,3 puntos porcentuales al daño físico final antes del redondeo. A media vida da +15 % y el bono se acerca al +30 % al quedarse con muy pocos PV. La pasiva afecta a golpes básicos, habilidades físicas y contraataques. Los hechizos, pergaminos y daños periódicos usan sus propias fórmulas.

## Fase 7 · Botín y calidades

Cada enemigo vencido tiene una tirada independiente para soltar como máximo un objeto. Si la supera, una segunda tirada elige el objeto según los pesos condicionados de la tabla. El botín se recoge por unidad en Recompensas. Si se avanza con unidades pendientes, se deja atrás ese botín.

| Enemigo | Probabilidad de soltar | Reparto si suelta |
| --- | ---: | --- |
| Goblin | 35 % | Daga curva 45 %, capucha 30 %, armadura ligera 25 % |
| Goblin arquero | 40 % | Arco goblin 75 %, daga curva 25 % |
| Esqueleto | 40 % | Espada 40 %, escudo 35 %, armadura antigua 25 % |
| Chamán | 50 % | Vara de chamán 30 %, túnica de chamán 30 %, Proyectil mágico 40 % |
| Ogro | 65 % | Garrote 50 %, armadura pesada 30 %, botas reforzadas 20 % |
| Caballero oscuro | 75 % | Espada maldita 40 %, armadura oscura 35 %, casco oscuro 25 % |
| Nigromante | 60 % | Vara de osario 35 %, capucha funeraria 25 %, daga ritual 10 %, poción dorada 30 % |
| Gólem rúnico | 70 % | Guantes rúnicos 40 %, coraza rúnica 35 %, Protección divina 25 % |
| Verdugo espectral | 65 % | Hacha espectral 40 %, yelmo espectral 35 %, Cura mayor 25 % |
| Dragón | 0 % | — |

| Calidad | Probabilidad | Multiplicador | Venta sobre el valor base | Recompra sobre el valor base |
| --- | ---: | ---: | ---: | ---: |
| Desgastada | 35 % | 0,75 | 30 % | 60 % |
| Normal | 50 % | 1,00 | 50 % | 100 % |
| Superior | 15 % | 1,25 | 75 % | 150 % |

Las bonificaciones se redondean al entero más próximo por magnitud y conservan su signo. Cada calidad tiene un nombre propio. Los requisitos se mantienen y los precios se calculan directamente a partir del valor base. El documento fijaba el precio de varios objetos especiales sin definir sus atributos. Para ellos se eligieron los siguientes valores.

| Objeto especial | Bonificaciones normales | Requisito |
| --- | --- | --- |
| Espada del esqueleto | +6 FUE | 12 FUE |
| Escudo | +2 DEF | 10 FUE, arma de una mano y clase compatible |
| Armadura antigua | +7 DEF, +2 RES, −1 VEL | 12 FUE |
| Espada maldita | +14 FUE | 20 FUE |
| Casco oscuro | +3 DEF, +2 RES | 16 FUE |

Las seis piezas nuevas se consiguen como botín o mediante Robar. Se pueden vender y recomprar como el resto del equipo. Los consumibles de estos enemigos usan las versiones de la tienda.

| Pieza | Bonos en calidad normal | Requisito base | Valor base |
| --- | --- | --- | ---: |
| Vara de osario | +10 INT, dos manos, Descarga fúnebre por 7 PC | 18 INT | 200 MO |
| Capucha funeraria | +1 DEF y +2 RES | — | 75 MO |
| Guantes rúnicos | +1 DEF y +2 RES | — | 100 MO |
| Coraza rúnica | +13 DEF y −4 VEL | 20 FUE | 220 MO |
| Hacha espectral | +11 FUE, dos manos, Sentencia espectral por 6 PC con 25 % de penetración | 18 FUE | 210 MO |
| Yelmo espectral | +2 DEF y +2 puntos porcentuales de esquiva | 14 FUE | 105 MO |

## Fase 8 · Tienda y economía

La tienda se abre entre combates. Tiene cuatro apartados, **Armas**, **Armaduras**, **Pociones** y **Pergaminos**. Las armas, armaduras y pociones del catálogo habitual son ilimitadas. Fuera del modo Pruebas, cada hechizo tiene una unidad de catálogo por partida. Una vez comprada, aparece como agotada aunque luego se venda o consuma el objeto. Los artículos vendidos aparecen en el mismo apartado como segunda mano. Recomprar retira exactamente esa unidad y conserva su UUID, variante, estadísticas y precio de recompra.

La partida empieza con 0 MO. Una poción se vende por la mitad del precio habitual redondeada al alza y se recompra al precio habitual. Las reglas de venta y recompra de equipo están en `Calidad`. Cada pergamino común se vende por la mitad de su precio de catálogo redondeada al alza y se recompra por el doble de su precio de venta.

## Fase 9 · Pociones y pergaminos

| Poción | Recuperación | Precio |
| --- | --- | ---: |
| Roja | 80 PV | 40 MO |
| Verde | 50 PC | 40 MO |
| Azul | 50 PV y 30 PC | 55 MO |
| Dorada | Revive a un héroe caído con el 25 % de sus PV máximos | 120 MO |

Las pociones se pueden usar en combate y durante la preparación. En combate, quien usa una poción gasta su acción. Las pociones de recuperación se aplican a un aliado vivo, incluido el propio usuario. Un héroe vivo puede revitalizar a un aliado caído en combate o durante la preparación. En combate, el héroe revivido vuelve a actuar en el siguiente ciclo. La recuperación se limita a los PV y PC máximos. Un consumo que falla la validación conserva el objeto.

Los pergaminos se usan en combate, requieren la INT base indicada y gastan una acción sin consumir PC. Los ofensivos tienen potencia fija, aciertan sin tirada, no hacen crítico y se reducen por RES y protección. Los de área se aplican a los enemigos vivos y las curaciones a los aliados vivos. El chamán puede soltar el pergamino Proyectil mágico. Cada hechizo tiene una única versión con los mismos efectos y requisitos al comprarlo, encontrarlo o robarlo.

| Pergamino | Efecto | INT base | Precio fijo |
| --- | --- | ---: | ---: |
| Proyectil mágico | 65 de daño individual | 5 | 65 MO |
| Cura | 90 PV a un aliado | 6 | 75 MO |
| Cura mayor | 120 PV a un aliado | 8 | 100 MO |
| Llamarada | 100 de daño individual | 10 | 110 MO |
| Ventisca | 65 de daño de área | 12 | 140 MO |
| Cura grupal | 70 PV por aliado vivo | 14 | 150 MO |
| Tormenta eléctrica | 80 de daño de área | 15 | 170 MO |
| Protección divina | 50 % de reducción de daño para el equipo durante dos turnos | 16 | 160 MO |
| Cura total | 100 % de los PV máximos a un aliado vivo | 18 | 180 MO |
| Meteorito | 140 de daño de área | 22 | 250 MO |

## Fase 10 · Experiencia, niveles y recompensas

En cada victoria anterior al dragón se ingresan automáticamente el oro y la experiencia. Cada héroe que esté en pie al terminar recibe la misma experiencia. El botín queda disponible para recoger. Cada ronda liquida sus recompensas una sola vez. Derrotar al dragón otorga la victoria inmediatamente y conserva el oro, la experiencia y el inventario sin añadir nuevas recompensas.

- Experiencia por ronda = `75 + 18 × (ronda − 1)`. En Experto y Pruebas, la ronda 14 da 62 EXP adicionales, para un total de 2.750 EXP y nivel 12 antes del dragón para los héroes que sobreviven a todas las rondas.
- Experiencia para subir = `75 + 35 × (nivel actual − 1)`. El sobrante pasa al siguiente nivel.
- Oro = el mayor valor entre `60 + 13 × ronda` y `95 + 14 × ronda + 12 × supervivientes − 2 × acciones − 6 × consumibles`. Se cuenta cada acción de héroe o enemigo como un turno. Los golpes múltiples y contraataques no añaden turnos, y solo se cuentan los consumibles usados durante ese combate.
- Cada subida concede tres puntos. Al alcanzar el nivel 12 se conceden cinco. Un punto de PV añade 12 PV máximos y uno de PC añade 8 PC máximos. El resto de atributos gana una unidad base por punto. Invertir en PV o PC también suma ese incremento al recurso actual de un héroe vivo.
- Por cada nivel, un héroe vivo recupera hasta 30 PV y 20 PC. Un héroe caído mantiene 0 PV tras subir de nivel o invertir puntos.

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
| `iniciar` | `dificultad` opcional (`PRUEBAS`, `FACIL`, `DIFICIL` o `EXPERTO`). El valor por defecto es `FACIL`, que se muestra como Normal. |
| `accion` | `turno`, `actor`, `habilidad` y `objetivo` cuando sea individual |
| `avanzar` | `ronda`, con la ronda recién ganada |
| `recoger` | `objeto`, UUID del botín |
| `equipar` | `actor`, `objeto` |
| `desequipar` | `actor`, `ranura` (`ARMADURAS`, `GUANTES`, `BOTAS`, `CABEZA` o `ESCUDO`). El campo anterior `categoria: ARMADURAS` sigue apuntando al torso. |
| `comprar` | `codigo` de catálogo o `objeto` de segunda mano |
| `vender` | `objeto` |
| `consumir` | `actor`, `objeto`, `turno` en combate, `objetivo` cuando sea individual |
| `atributos` | `actor`, `atributo`, `cantidad`. Los PC se envían con el código `PM` de la API. |
| `reiniciar` | Ninguno. Cierra el identificador anterior y devuelve otra partida. |

Ejemplo de cuerpo para seleccionar el equipo tras crear una partida con versión 0.

```json
{"version":0,"clases":["ARQUERO","SANADOR","GUERRERO"]}
```

## Fase 12 · Interfaz web

La portada permite crear una partida y seleccionar tres clases. En combate se muestran los dos bandos, barras de PV y PC, actor actual, orden pendiente, estados, acciones con objetivos válidos e historial. El campamento contiene Recompensas, Inventario, Equipamiento, Tienda y Nivel. La tienda compara con la ranura equipada del héroe elegido. Victoria y derrota permiten reiniciar. Al recargar, el navegador recupera el progreso si el servidor sigue ejecutándose.

Antes del primer combate se puede escoger Pruebas, Normal, Difícil o Experto. Normal está seleccionada por defecto y usa el equilibrio original. En Difícil los enemigos tienen ×1,25 PV e infligen ×1,50 de daño. En Experto tienen ×1,30 PV e infligen ×2,00 de daño. En Pruebas todos los enemigos tienen 1 PV y la partida tiene oro ilimitado. Todo el catálogo fijo tiene existencias ilimitadas, incluidos los pergaminos. Cada artículo de segunda mano se recompra por unidad. La dificultad se conserva durante toda la partida.

Los archivos `index.html`, `estilos.css` y `juego.js` se sirven desde `src/main/resources/static`. Se usan símbolos provisionales y estilos locales sin imágenes externas ni frameworks. El cliente usa `fetch` para obtener el estado y enviar las decisiones del jugador. El backend calcula todos los resultados y valida las operaciones.

## Fase 13 · Pruebas y equilibrio

Ejecuta `./mvnw clean verify` para compilar, probar y empaquetar el JAR en `target/`. Puedes arrancar el resultado directamente con `java -jar target/rpg-battle-gauntlet-0.1.0-SNAPSHOT.jar`.

JUnit cubre la selección, las estadísticas, los turnos, las fórmulas, la IA, la progresión por diez rondas y por las quince de Experto y Pruebas, las recompensas, el inventario, los requisitos, el botín controlado, el consumo, la tienda, las operaciones HTTP y los reintentos simultáneos. Las pruebas de reglas inyectan el azar para controlar los resultados. La prueba de progresión debilita los enemigos deliberadamente para comprobar el flujo de estados. Las simulaciones de equilibrio usan combates reales, tienda y atributos, sin recoger botín.

`CampanaTest` reproduce 30 partidas con semillas 0 a 29 y un equipo de arquero, sanador y guerrero. La estrategia mejora el arma del arquero y del guerrero, invierte en PV y usa pociones, curación y pergaminos de tienda para el tramo final. Ignora todo el botín. Las 30 ejecuciones prueban que esa estrategia puede completar la campaña con las reglas y recursos de tienda. Probar otras composiciones y estrategias permitiría medir un equilibrio más amplio.

También se verificó el JAR en Chrome. El recorrido creó una partida, eligió héroes, ganó el primer combate, visitó los cuatro apartados de la tienda, compró una poción y asignó un punto de atributo. Después recargó la página, comprobó el diseño a 360 px, avanzó a la ronda 2 y reinició la partida. El resultado de la compilación final fue de 71 pruebas sin fallos ni errores.
