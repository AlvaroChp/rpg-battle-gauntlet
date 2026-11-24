'use strict';

const juego = document.querySelector('#juego');
const resumen = document.querySelector('#resumen');
const aviso = document.querySelector('#aviso');
const ruta = '/api/partidas';
const clave = 'gauntlet-partida';
const nombres = {
    GUERRERO: 'Guerrero', MAGO: 'Mago', ARQUERO: 'Arquera', SANADOR: 'Sanadora', PICARO: 'Pícaro',
    GOBLIN: 'Goblin', GOBLIN_ARQUERO: 'Goblin arquero', ESQUELETO: 'Esqueleto', CHAMAN: 'Chamán', OGRO: 'Ogro',
    CABALLERO_OSCURO: 'Caballero oscuro', DRAGON: 'Dragón',
    NIGROMANTE: 'Nigromante', GOLEM_RUNICO: 'Gólem rúnico', VERDUGO_ESPECTRAL: 'Verdugo espectral',
    ARMAS: 'Armas', ARMADURAS: 'Armaduras', POCIONES: 'Pociones', PERGAMINOS: 'Pergaminos',
    NORMAL: 'Normal', INFERIOR: 'Desgastada', SUPERIOR: 'Superior',
    DEFENSA: 'Defensa', INTERCEPTANDO: 'Listo para interceptar', BENDICION: 'Bendición', EVASION: 'Esquiva',
    RALENTIZACION: 'Ralentización', ENVENENADO: 'Veneno', QUEMADO: 'Quemadura', DEFENSA_DEBILITADA: 'Defensa debilitada',
    CARGA_FURIA_ANIMAL: 'Prepara Furia animal', CARGA_INCENDIO_CENIZA: 'Prepara Incendio de ceniza',
    CARGA_ONDA_RUNICA: 'Prepara Onda rúnica', MARCA_ESPECTRAL: 'Marca espectral',
    GUARDIA_OSEA_ACTIVA: 'Guardia ósea', GUARDIA_OSCURA_ACTIVA: 'Guardia oscura',
    CONTRAATAQUE_LISTO: 'Contraataque preparado', PROTECCION_50: 'Protección del 50 %'
};
const iconos = { GUERRERO: '⚔', MAGO: '✦', ARQUERO: '➶', SANADOR: '✚', PICARO: '◆',
    GOBLIN: '♦', ESQUELETO: '☠', CHAMAN: '✷', OGRO: '⬟', CABALLERO_OSCURO: '♜', DRAGON: '♞' };
const spritesCombate = {
    GUERRERO: 'heroes/guerrero.png', MAGO: 'heroes/mago.png', ARQUERO: 'heroes/arquero.png', SANADOR: 'heroes/sanador.png',
    PICARO: 'heroes/picaro.png',
    GOBLIN: 'enemigos/goblin.png', GOBLIN_ARQUERO: 'enemigos/goblin_arquero.png', ESQUELETO: 'enemigos/esqueleto.png', CHAMAN: 'enemigos/chaman.png', OGRO: 'enemigos/ogro.png',
    CABALLERO_OSCURO: 'enemigos/caballero_oscuro.png', DRAGON: 'enemigos/dragon.png',
    NIGROMANTE: 'enemigos/nigromante.png', GOLEM_RUNICO: 'enemigos/golem_runico.png',
    VERDUGO_ESPECTRAL: 'enemigos/verdugo_espectral.png'
};
const biografias = {
    GUERRERO: {
        titulo: 'Therakos - Guerrero',
        parrafos: [
            'Antiguo capitán de los Caballeros de la Luna Quebrada, Therakos renegó del culto tras descubrir las verdaderas intenciones del Padre Fundador. Acusado de herejía, se vio obligado a acabar con la vida de su mejor amigo en defensa propia.',
            'Desde entonces vive como mercenario, ocultando bajo su armadura los tatuajes que delatan su pasado como miembro de la orden. Ofrece su espada a cualquiera que pueda pagar por sus servicios, pero se niega a servir a tirano alguno.'
        ]
    },
    MAGO: {
        titulo: 'Vaelthar - Mago',
        parrafos: [
            'Misterioso erudito cuyo dominio de la magia supera al de los más prestigiosos estudiosos. Posee un conocimiento extraordinario de la desaparecida civilización Kryptoi, sus costumbres y sus artes arcanas.',
            'Lo que nadie sabe es que Vaelthar es un Kryptoi inmortal que ha sobrevivido al Cataclismo. Ocultando su verdadera naturaleza, lleva siglos recorriendo el mundo bajo distintas identidades, custodiando secretos de una civilización que todos creen extinta.'
        ]
    },
    ARQUERO: {
        titulo: 'Lyra - Arquera',
        parrafos: [
            'Criada en una pequeña comunidad de cazadores, Lyra aprendió desde temprana edad a orientarse en los bosques, rastrear animales y manejar el arco. Su extraordinaria puntería le valió un puesto entre los exploradores encargados de proteger las fronteras de su territorio.',
            'Tras años al servicio de su pueblo, decidió abandonar su hogar y recorrer el mundo, ganándose la vida como exploradora y mercenaria. Su conocimiento del terreno y su capacidad para anticiparse al peligro la convierten en una valiosa compañera de viaje.'
        ]
    },
    SANADOR: {
        titulo: 'Dabria - Sanadora',
        parrafos: [
            'Originaria de Kael-Seriv, Dabria es una eminente estudiosa de la magia, especializada en las artes de la restauración y la creación de barreras energéticas. Su profundo conocimiento de la antigua civilización Kryptoi la llevó a investigar los acontecimientos que desencadenaron el Cataclismo.',
            'Tras descubrir una verdad inquietante en unos documentos prohibidos, decidió abandonar la capital. Desde entonces recorre el mundo en busca de respuestas, empleando su dominio del Eco para proteger y sanar a sus compañeros.'
        ]
    },
    PICARO: {
        titulo: 'Lykos - Pícaro',
        parrafos: [
            'Huérfano desde temprana edad, Lykos creció entre los callejones de Vhaldris, una próspera ciudad comercial donde la opulencia de la nobleza contrasta con la miseria de los barrios bajos. Fue acogido por el gremio de ladrones, cuyos miembros le dieron el nombre por el que se le conoce. Ni él mismo conoce su verdadera identidad ni sus orígenes.',
            'Tras abandonar el gremio, comenzó a recorrer el mundo como mercenario. Aunque desconfía de las grandes promesas y los ideales heroicos, posee su propio código de honor y jamás traiciona a quienes considera sus compañeros.'
        ]
    }
};
const atributosBase = { fuerza: 'FUE', defensa: 'DEF', inteligencia: 'INT', resistencia: 'RES',
    velocidad: 'VEL', destreza: 'DES' };
const atributos = { ...atributosBase, critico: 'Crítico', esquiva: 'Esquiva', potenciaMagica: 'Potencia mágica' };
const ranurasEquipo = { ARMAS: 'Arma', ESCUDO: 'Escudo', CABEZA: 'Cabeza',
    ARMADURAS: 'Torso', GUANTES: 'Guantes', BOTAS: 'Botas' };
const clasesConEscudo = ['GUERRERO', 'PICARO'];
const categorias = ['ARMAS', 'ARMADURAS', 'POCIONES', 'PERGAMINOS'];
const paneles = { recompensas: 'Recompensas', estado: 'Estado', inventario: 'Inventario', equipo: 'Equipamiento', tienda: 'Tienda', nivel: 'Nivel' };
const dificultades = { PRUEBAS: 'Pruebas', FACIL: 'Normal', DIFICIL: 'Difícil', EXPERTO: 'Experto' };
const observarNombresCombate = new ResizeObserver(entradas => {
    for (const { target: titulo, contentRect } of entradas) {
        titulo.style.removeProperty('font-size');
        const anchoTexto = titulo.firstElementChild.getBoundingClientRect().width;
        if (anchoTexto > contentRect.width && contentRect.width > 0) {
            const tamanoBase = Number.parseFloat(getComputedStyle(titulo).fontSize);
            titulo.style.fontSize = `${tamanoBase * contentRect.width / anchoTexto}px`;
        }
    }
});
let partida;
let perfiles = [];
let seleccion = [];
let dificultadElegida = 'FACIL';
let panel = 'recompensas';
let categoria = 'ARMAS';
let heroeElegido;
let accionElegida;
let ocupado = false;
let tiempoAviso;

function escapar(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, signo => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[signo]);
}

function nombre(tipo) {
    return nombres[tipo] ?? String(tipo ?? '').replaceAll('_', ' ').toLowerCase();
}

function informar(mensaje) {
    clearTimeout(tiempoAviso);
    aviso.textContent = mensaje;
    aviso.hidden = !mensaje;
    if (mensaje) tiempoAviso = setTimeout(() => { aviso.hidden = true; }, 6500);
}

async function api(url, datos) {
    const respuesta = await fetch(url, datos === undefined ? {} : {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(datos)
    });
    const contenido = await respuesta.json();
    if (!respuesta.ok) {
        const mensaje = contenido.mensaje ?? 'No se pudo completar la petición';
        const error = new Error(mensaje.replace(/\bPM\b/g, 'PC'));
        error.estado = respuesta.status;
        throw error;
    }
    return contenido;
}

function actualizar(nueva) {
    if (nueva.id !== partida?.id || nueva.estado !== partida?.estado) {
        panel = 'recompensas';
        accionElegida = null;
    }
    partida = nueva;
    localStorage.setItem(clave, partida.id);
}

async function operar(operacion, datos = {}) {
    if (ocupado) return false;
    ocupado = true;
    juego.setAttribute('aria-busy', 'true');
    informar('');
    try {
        const nueva = operacion === 'crear'
            ? await api(ruta, {})
            : await api(`${ruta}/${partida.id}/${operacion}`, { version: partida.version, ...datos });
        const eventos = nueva.id === partida?.id ? nueva.historial.slice(partida.historial.length) : [];
        if (eventos.length && partida.estado !== 'COMBATE' && nueva.estado === 'COMBATE') {
            actualizar(nueva);
            mostrar();
        }
        if (eventos.length) await reproducirAcciones(eventos);
        if (partida !== nueva) actualizar(nueva);
        return true;
    } catch (error) {
        informar(error.message);
        if (error.estado === 409 && partida) {
            try {
                actualizar(await api(`${ruta}/${partida.id}`));
            } catch (errorConsulta) {
                if (errorConsulta.estado === 404) volverAlInicio();
            }
        } else if (error.estado === 404) {
            volverAlInicio();
        }
        return false;
    } finally {
        ocupado = false;
        juego.setAttribute('aria-busy', 'false');
        mostrar();
    }
}

function agruparAcciones(eventos) {
    const acciones = [];
    for (const evento of eventos) {
        const anterior = acciones.at(-1);
        if (anterior && anterior[0].turno === evento.turno && anterior[0].actor === evento.actor
            && anterior[0].habilidad === evento.habilidad) {
            anterior.push(evento);
        } else {
            acciones.push([evento]);
        }
    }
    return acciones;
}

function textoAccion(accion, anterior) {
    if (!accion) return '';
    const intercepto = anterior?.[0].habilidad === 'golpe_interceptado'
        && anterior[0].turno === accion[0].turno;
    return (intercepto ? anterior.concat(accion) : accion).map(textoEvento).join(' ');
}

async function reproducirAcciones(eventos) {
    const aviso = juego.querySelector('.ultima-accion');
    if (!aviso) return;
    const figuras = new Map([...juego.querySelectorAll('[data-combatiente]')].map(luchador =>
        [luchador.dataset.combatiente, luchador.querySelector('.figura-combate')]));
    let anterior;
    for (const accion of agruparAcciones(eventos)) {
        aviso.textContent = textoAccion(accion, anterior);
        aviso.scrollTop = 0;
        anterior = accion;
        await animarFiguras([figuras.get(accion[0].actor)], 'accion-en-curso');

        const resultados = new Map();
        const sumarResultado = (id, danio, curacion, esquivado = false) => {
            const figura = figuras.get(id);
            if (!figura || !(danio || curacion || esquivado)) return;
            const resultado = resultados.get(figura) ?? { danio: 0, curacion: 0, esquivado: false };
            resultado.danio += danio;
            resultado.curacion += curacion;
            resultado.esquivado ||= esquivado;
            resultados.set(figura, resultado);
        };
        for (const evento of accion) {
            sumarResultado(evento.objetivo, evento.danio, evento.curacion, evento.esquivado);
            sumarResultado(evento.actor, 0, evento.curacionActor ?? 0);
        }
        const indicadores = [];
        const golpeados = [];
        const curados = [];
        const esquivaron = [];
        for (const [figura, resultado] of resultados) {
            const indicador = document.createElement('div');
            indicador.className = 'resultado-combate';
            indicador.setAttribute('aria-hidden', 'true');
            if (resultado.danio) {
                indicador.append(etiquetaResultado(`−${resultado.danio} PV`, 'danio'));
                golpeados.push(figura);
            }
            if (resultado.curacion) {
                indicador.append(etiquetaResultado(`+${resultado.curacion} PV`, 'curacion'));
                if (!resultado.danio) curados.push(figura);
            }
            if (resultado.esquivado) {
                indicador.append(etiquetaResultado('Esquiva', 'esquiva'));
                if (!resultado.danio && !resultado.curacion) esquivaron.push(figura);
            }
            const retrato = figura.querySelector('.retrato-combate');
            const estados = retrato.querySelector('.estados-combate');
            (estados ?? retrato).append(indicador);
            indicadores.push(indicador);
        }
        await Promise.all([
            animarFiguras(golpeados, 'golpe-recibido'),
            animarFiguras(curados, 'curacion-recibida'),
            animarFiguras(esquivaron, 'esquiva-realizada')
        ]);
        indicadores.forEach(indicador => indicador.remove());
    }
}

function etiquetaResultado(texto, clase) {
    const etiqueta = document.createElement('span');
    etiqueta.className = clase;
    etiqueta.textContent = texto;
    return etiqueta;
}

async function animarFiguras(figuras, clase) {
    const animaciones = [];
    for (const figura of figuras) {
        if (!figura) continue;
        figura.classList.add(clase);
        animaciones.push(...figura.getAnimations().map(animacion => animacion.finished));
    }
    await Promise.allSettled(animaciones);
    for (const figura of figuras) figura?.classList.remove(clase);
}

function volverAlInicio() {
    partida = null;
    seleccion = [];
    localStorage.removeItem(clave);
    mostrar();
}

function boton(texto, accion, valor = '', opciones = '') {
    return `<button type="button" data-accion="${accion}" data-valor="${escapar(valor)}" ${opciones}>${escapar(texto)}</button>`;
}

function mostrar() {
    observarNombresCombate.disconnect();
    resumen.innerHTML = partida ? `
        ${partida.ronda ? `<span>${dificultades[partida.dificultad] ?? dificultades.FACIL}</span><span>Ronda ${partida.ronda} de ${partida.totalRondas}</span><span>${partida.oroIlimitado ? '∞' : partida.oro} MO</span>` : ''}
        ${boton('Reiniciar', 'reiniciar')}` : '';
    if (!partida) {
        juego.innerHTML = `<section class="inicio">
            <div class="insignia" aria-hidden="true">⚔</div><h1>RPG Battle Gauntlet</h1>
            <p>Reúne a tres héroes y derrota al dragón. Juega diez rondas o amplía el desafío a quince en Experto o Pruebas.</p>
            ${boton('Nueva partida', 'crear', '', 'class="principal"')}
        </section>`;
    } else if (partida.estado === 'SELECCION_PERSONAJES') {
        mostrarSeleccion();
    } else if (partida.estado === 'COMBATE') {
        mostrarCombate();
    } else if (partida.estado === 'ENTRE_COMBATES') {
        mostrarCampamento();
    } else {
        const victoria = partida.estado === 'VICTORIA';
        juego.innerHTML = `<section class="inicio">
            <div class="insignia" aria-hidden="true">${victoria ? '♜' : '⚔'}</div>
            <h1>${victoria ? 'Victoria' : 'Derrota'}</h1>
            <p>${victoria ? `El dragón ha caído. Has superado las ${partida.totalRondas} rondas.` : `Tu equipo cayó en la ronda ${partida.ronda}.`}</p>
            ${boton('Volver a jugar', 'reiniciar', '', 'class="principal"')}
        </section>${historial()}`;
    }
}

function mostrarSeleccion() {
    const detallesAbiertos = new Set([...juego.querySelectorAll('.detalle-seleccion[open]')].map(detalle => detalle.dataset.detalle));
    const disponibles = partida.dificultadesDisponibles ?? (partida.dificultad ? ['FACIL', 'DIFICIL', 'EXPERTO'] : ['FACIL']);
    const modos = Object.keys(dificultades).filter(codigo => disponibles.includes(codigo));
    juego.innerHTML = `<h1>Elige a tus héroes</h1><p class="ayuda">${seleccion.length} de 3 clases seleccionadas</p>
        <div class="rejilla seleccion-heroes">${perfiles.map(({clase, base}) => {
            const { titulo, parrafos } = biografias[clase];
            return `<article class="tarjeta ficha-seleccion">
                <div class="retrato-seleccion"><img src="imagenes/${spritesCombate[clase]}" alt=""></div>
                <h2>${escapar(titulo)}</h2>
                <small>${base.pvMaximos} PV · ${base.pmMaximos} PC · Esquiva ${base.esquiva} · Crítico ${base.destreza ?? base.critico}</small>
                <small>${Object.entries(atributosBase).filter(([clave]) => clave in base)
                    .map(([clave, etiqueta]) => `${etiqueta} ${base[clave]}`).join(' · ')}</small>
                <details class="detalle-seleccion" data-detalle="${clase}"${detallesAbiertos.has(clase) ? ' open' : ''}>
                    <summary><span>Ver historia</span><span>Ocultar historia</span></summary>
                    ${parrafos.map(parrafo => `<p>${escapar(parrafo)}</p>`).join('')}
                </details>
                ${boton('Elegir', 'clase', clase, `aria-label="Elegir a ${escapar(titulo)}" aria-pressed="${seleccion.includes(clase)}"`)}
            </article>`;
        }).join('')}
            <article class="tarjeta ficha-seleccion">
                <h2>Cómo jugar</h2>
                <details class="detalle-seleccion" data-detalle="guia"${detallesAbiertos.has('guia') ? ' open' : ''}>
                    <summary><span>Ver conceptos básicos</span><span>Ocultar conceptos básicos</span></summary>
                    <p><strong>Equipo y turnos.</strong> Elige tres héroes y supera diez rondas, o quince en Experto o Pruebas. La velocidad (VEL) decide el orden. En cada turno puedes atacar, usar una habilidad o un objeto, o defenderte.</p>
                    <p><strong>Vida y concentración.</strong> Los Puntos de Vida (PV) son la salud de nuestro personaje. Con 0 PV, el héroe cae. Los puntos de concentración (PC) se gastan al usar habilidades. La poción roja recupera 80 PV, la verde recupera 50 PC y la azul recupera 50 PV y 30 PC. El ataque básico y Defenderse son gratuitos.</p>
                    <p><strong>Interceptar.</strong> Therakos puede gastar 4 PC para proteger el próximo golpe que reciba o ponerse en el lugar de un aliado ante un ataque individual. La postura termina al empezar su siguiente turno. Un escudo equipado eleva la reducción de Defenderse y del golpe interceptado del 40 % al 65 %.</p>
                    <p><strong>Daño físico.</strong> La fuerza (FUE) aumenta el daño y la defensa (DEF) lo reduce.</p>
                    <p><strong>Magia.</strong> La inteligencia (INT) mejora el daño mágico y la curación. La resistencia (RES) reduce el daño mágico. Los ataques mágicos siempre aciertan y carecen de crítico.</p>
                    <p><strong>Quemadura y veneno.</strong> El fuego y Flecha envenenada pueden aplicar un estado con un 50 % de probabilidad, o un 25 % con Defenderse o Bendición. Incendio de ceniza quema con un 100 % de probabilidad, o un 50 % con alguna de esas protecciones. El daño dura dos turnos del objetivo.</p>
                    <p><strong>Esquiva.</strong> Es la probabilidad de evitar un golpe físico por completo. Una prenda como la capucha puede aumentarla. La precisión del arma o del ataque puede reducir la esquiva del objetivo para ese golpe.</p>
                    <p><strong>Crítico.</strong> Cuando un golpe físico acierta, puede infligir un 50 % más de daño. Cada punto de destreza (DES) aporta un punto porcentual de crítico base. El equipo y el ataque pueden añadir bonos, como los de la daga y su Estocada rápida.</p>
                    <p><strong>Campamento.</strong> Entre rondas puedes recoger botín, comprar equipo y gastar puntos de nivel. Cada arma concede un ataque propio. La Poción dorada revive a un aliado caído con el 25 % de sus PV máximos.</p>
                </details>
            </article>
        </div>
        <div class="fila"><label>Dificultad <select id="dificultad">${modos.map(codigo =>
            `<option value="${codigo}" ${codigo === dificultadElegida ? 'selected' : ''}>${dificultades[codigo]}</option>`).join('')}</select></label>
            ${boton('Iniciar combate', 'iniciar', '', `class="principal" ${seleccion.length === 3 ? '' : 'disabled'}`)}</div>`;
}

function barra(etiqueta, actual, maximo, clase = '') {
    return `<div class="barra"><span>${actual}/${maximo} ${etiqueta}</span>
        <progress class="${clase}" aria-label="${etiqueta}" value="${actual}" max="${maximo || 1}"></progress></div>`;
}

function luchador(datos, enCampamento = false) {
    const turnoActual = !enCampamento && partida.orden[0] === datos.id;
    const indicarTurno = turnoActual && datos.pv > 0 && partida.heroes.some(heroe => heroe.id === datos.id);
    const sprite = spritesCombate[datos.tipo];
    const imagen = sprite
        ? `<img class="sprite-combate" src="imagenes/${sprite}" alt="">`
        : `<span class="icono" aria-hidden="true">${iconos[datos.tipo]}</span>`;
    const estados = Object.entries(datos.estados).map(([estado, turnos]) =>
        `<small>${escapar(nombre(estado))}${estado === 'MARCA_ESPECTRAL' ? '' : ` · ${turnos} ${turnos === 1 ? 'turno' : 'turnos'}`}</small>`).join('');
    return `<article class="luchador ${datos.pv === 0 ? 'caido' : ''}" data-tipo="${datos.tipo}" data-combatiente="${datos.id}">
        <div class="figura-combate">
            <div class="retrato-combate">
                ${imagen}
                ${indicarTurno ? '<span class="indicador-turno" aria-hidden="true"></span>' : ''}
                ${estados ? `<div class="estados-combate">${estados}</div>` : ''}
            </div>
        </div>
        <div class="tarjeta ${turnoActual ? 'activo' : ''}">
            <h3><span>${nombre(datos.tipo)}</span></h3>
            ${enCampamento ? `<small>Nivel ${datos.nivel}</small>` : ''}
            ${barra('PV', datos.pv, datos.base.pvMaximos)}${barra('PC', datos.pm, datos.base.pmMaximos, 'magia')}
            ${enCampamento ? `<small>${datos.pv > 0 ? 'En pie' : 'Fuera de combate'}</small>` : '<small class="aviso-caida">Fuera de combate</small>'}
            ${enCampamento ? `<dl class="equipo-estado">
                ${Object.entries(ranurasEquipo).map(([ranura, etiqueta]) =>
                    `<dt>${etiqueta}</dt><dd>${escapar(datos.equipo[ranura]?.nombre ?? 'Sin equipar')}</dd>`).join('')}
            </dl>` : ''}
        </div>
    </article>`;
}

const iconosPorObjeto = {
    roja: 'pociones/pocion_roja.png',
    verde: 'pociones/pocion_verde.png',
    azul: 'pociones/pocion_azul.png',
    revitalizacion: 'pociones/pocion_dorada.png',
    proyectil: 'pergaminos/proyectil_magico.png',
    cura: 'pergaminos/cura.png',
    cura_mayor: 'pergaminos/cura.png',
    cura_grupal: 'pergaminos/cura.png',
    cura_total: 'pergaminos/cura.png',
    proteccion: 'pergaminos/proteccion_divina.png',
    llamarada: 'pergaminos/llamarada.png',
    ventisca: 'pergaminos/ventisca.png',
    tormenta: 'pergaminos/tormenta_electrica.png',
    meteorito: 'pergaminos/meteorito.png',
    requiem_dragon: 'pergaminos/dragon.png',
    vara_avanzada: 'armas/vara_avanzada.png?v=vara-avanzada',
    ropa: 'armaduras/ropa_ligera.png',
    capucha: 'armaduras/capucha.png',
    capucha_funeraria: 'armaduras/capucha.png',
    botas: 'armaduras/botas.png',
    casco: 'armaduras/yelmo.png',
    casco_oscuro: 'armaduras/yelmo.png',
    yelmo_espectral: 'armaduras/yelmo.png',
    escudo: 'armaduras/escudo.png',
    sombrero_mago: 'armaduras/sombrero_mago.png',
    tunica_aprendiz: 'armaduras/tunica_mago.png',
    tunica_mago: 'armaduras/tunica_mago.png',
    tunica_chaman: 'armaduras/sombrero_mago.png'
};

const iconosPorFamilia = {
    ESPADA: 'armas/espada.png',
    DAGA: 'armas/daga_goblin.png',
    ARCO: 'armas/arco.png',
    BALLESTA: 'armas/ballesta.png',
    GARROTE: 'armas/garrote.png',
    VARA: 'armas/vara.png'
};

const iconosPorCategoria = {
    ARMADURAS: 'armaduras/armadura.png'
};

function iconoObjeto(objeto) {
    const ruta = iconosPorObjeto[objeto.codigo] ?? iconosPorFamilia[objeto.familiaArma]
        ?? iconosPorCategoria[objeto.categoria];
    return ruta ? `<img class="icono-objeto" src="imagenes/${ruta}" alt="" width="40" height="40">` : '';
}

const nombrePergaminoDragon = 'Pergamino de Dragón';
const nombrePocionDorada = 'Poción dorada';

function nombreObjeto(objeto) {
    if (objeto.codigo === 'requiem_dragon') return nombrePergaminoDragon;
    if (objeto.codigo === 'revitalizacion') return nombrePocionDorada;
    return objeto.nombre;
}

const etiquetasCombate = { ataque_basico: 'Ataque básico', defender: 'Defenderse', bola_fuego: 'Bola de fuego',
        interceptar: 'Interceptar', golpe_interceptado: 'Se interpone para proteger a',
        fuego_chaman: 'Bola de fuego', aliento_fuego: 'Aliento de fuego', barrido: 'Ataque de área', robar: 'Robar',
        corte: 'Corte amplio', estocada_precisa: 'Estocada precisa', tajo_demoledor: 'Tajo demoledor',
        corte_ultratumba: 'Corte de ultratumba', filo_maldicion: 'Filo de la maldición',
        estocada_rapida: 'Estocada rápida', puntada_traicionera: 'Puntada traicionera', corte_al_tendon: 'Corte al tendón', sangria_ritual: 'Sangría ritual',
        impacto: 'Impacto contundente',
        flecha_de_alcance: 'Flecha de largo alcance', destello_arcano: 'Destello arcano', descarga_astral: 'Descarga astral',
        eco_de_la_tormenta: 'Eco de la tormenta',
        flechazo_goblin: 'Flechazo goblin', flecha_envenenada: 'Flecha envenenada', veneno: 'Veneno', quemadura: 'Quemadura',
        preparar_furia_animal: 'Prepara Furia animal', furia_animal: 'Furia animal', furia_interrumpida: 'Furia animal interrumpida',
        descarga_funebre: 'Descarga fúnebre', reanimar: 'Reanimar', puno_de_piedra: 'Puño de piedra',
        preparar_onda_runica: 'Prepara Onda rúnica', onda_runica: 'Onda rúnica',
        tajo_espectral: 'Tajo espectral', marcar_sentencia: 'Marca espectral', sentencia_espectral: 'Sentencia espectral',
        barrido_espectral: 'Barrido espectral',
        preparar_incendio_ceniza: 'Prepara Incendio de ceniza', incendio_ceniza: 'Incendio de ceniza',
        'Réquiem del Dragón': nombrePergaminoDragon, 'Poción de revitalización': nombrePocionDorada };
function textoEvento(evento) {
    const actor = nombre(evento.tipoActor);
    const objetivo = nombre(evento.tipoObjetivo);
    if (evento.habilidad === 'golpe_interceptado') return `${actor} se interpone para proteger a ${objetivo}.`;
    const accion = etiquetasCombate[evento.habilidad] ?? evento.habilidad.replaceAll('_', ' ');
    const destino = evento.actor === evento.objetivo ? '' : ` → ${objetivo}`;
    const detalles = [];
    if (evento.habilidad === 'robar') {
        detalles.push(evento.objetoRobado ? `roba ${nombreObjeto(evento.objetoRobado)}` : 'intento fallido');
    }
    if (evento.esquivado) detalles.push(`${objetivo} esquiva`);
    if (evento.danio) detalles.push(`${evento.danio} de daño${evento.critico ? ' crítico' : ''}`);
    if (evento.curacion) detalles.push(`+${evento.curacion} PV`);
    if (evento.curacionActor) detalles.push(`${actor} recupera ${evento.curacionActor} PV`);
    if (evento.magiaRecuperada) detalles.push(`+${evento.magiaRecuperada} PC`);
    return [`${actor} · ${accion}${destino}`, ...detalles].join(' · ') + '.';
}

function historial() {
    const eventos = partida.historial.slice().reverse();
    return `<details class="panel" ${partida.estado === 'COMBATE' ? 'open' : ''}><summary>Historial de combate (${eventos.length})</summary>
        <ol class="registro">${eventos.map(evento =>
            `<li>${evento.turno} · ${escapar(textoEvento(evento))}</li>`).join('')}</ol></details>`;
}

function nombresDeLosSiguientesTurnos() {
    const vivos = partida.heroes.concat(partida.enemigos).filter(combatiente => combatiente.pv > 0);
    const combatientes = new Map(vivos.map(combatiente => [combatiente.id, {
        nombre: nombre(combatiente.tipo),
        velocidadBase: combatiente.base.velocidad + (combatiente.modificadoresEquipo?.velocidad ?? 0),
        turnosRalentizado: combatiente.estados.RALENTIZACION ?? 0
    }]));
    const velocidad = id => {
        const datos = combatientes.get(id);
        return Math.max(1, datos.velocidadBase - (datos.turnosRalentizado > 0 ? 4 : 0));
    };
    let pendientes = partida.orden.filter(id => combatientes.has(id));
    const nombres = [];
    for (let turno = 0; turno <= 4; turno++) {
        if (!pendientes.length) pendientes = [...combatientes.keys()].sort((a, b) => velocidad(b) - velocidad(a));
        const datos = combatientes.get(pendientes.shift());
        if (turno > 0) nombres.push(datos.nombre);
        if (datos.turnosRalentizado > 0) datos.turnosRalentizado--;
    }
    return nombres;
}

function mostrarCombate() {
    observarNombresCombate.disconnect();
    const actor = partida.heroes.find(heroe => heroe.id === partida.orden[0]);
    if (!actor) return;
    const consumibles = partida.inventario.filter(objeto => objeto.efecto);
    const aliadosCaidos = partida.heroes.filter(heroe => heroe.pv === 0);
    const dragonesVivos = partida.enemigos.filter(enemigo => enemigo.tipo === 'DRAGON' && enemigo.pv > 0);
    const objetivosRobo = new Set(partida.objetivosRobo ?? []);
    const opciones = [
        ...partida.habilidades.map(habilidad => ({ ...habilidad, texto: `${habilidad.nombre} · ${habilidad.costePm} PC`,
            avisoBloqueo: habilidad.id === 'robar' && !objetivosRobo.size ? 'Sin objetivos con botín' : '' })),
        ...consumibles.map(objeto => {
            const { tipo, objetivo } = objeto.efecto;
            const avisoBloqueo = actor.base.inteligencia < objeto.inteligenciaMinima ? 'INT insuficiente'
                : tipo === 'REVITALIZACION' && !aliadosCaidos.length ? 'Sin aliados caídos'
                : tipo === 'ANIQUILAR_DRAGON' && !dragonesVivos.length ? 'Requiere al dragón' : '';
            return { id: objeto.id, objetivo, objeto, avisoBloqueo,
                texto: nombreObjeto(objeto), descripcion: describir(objeto) };
        })
    ];
    const accion = opciones.find(opcion => opcion.id === accionElegida && !opcion.avisoBloqueo) ?? opciones[0];
    accionElegida = accion.id;
    const objetivos = accion.id === 'robar' ? partida.enemigos.filter(enemigo => objetivosRobo.has(enemigo.id))
        : accion.objeto?.efecto.tipo === 'ANIQUILAR_DRAGON' ? dragonesVivos
        : accion.objetivo === 'ALIADO_CAIDO' ? aliadosCaidos
        : accion.objetivo === 'ENEMIGO' ? partida.enemigos.filter(enemigo => enemigo.pv > 0)
        : accion.objetivo === 'ALIADO' ? partida.heroes.filter(heroe => heroe.pv > 0) : [];
    const apuntarACaido = accion.objetivo === 'ALIADO_CAIDO';
    const elegirObjetivo = objetivos.length ? `
        <label>${apuntarACaido ? 'Aliado caído' : 'Objetivo'}<select name="objetivo" required>${objetivos
            .map(objetivo => `<option value="${objetivo.id}">${nombre(objetivo.tipo)} · ${apuntarACaido ? 'Fuera de combate' : `${objetivo.pv} PV`}</option>`)
            .join('')}</select></label>` : '';
    const ultimoTurno = partida.historial.at(-1)?.turno;
    const eventosUltimoTurno = ultimoTurno >= partida.primerTurnoCombate
        ? partida.historial.filter(evento => evento.turno === ultimoTurno)
        : [];
    const acciones = agruparAcciones(eventosUltimoTurno);
    const escenario = Math.ceil(partida.ronda / 5);
    juego.innerHTML = `<div class="bandos escenario" data-escenario="${escenario}">
            <div class="ultima-accion" role="status" aria-label="Última acción">${escapar(textoAccion(acciones.at(-1), acciones.at(-2)))}</div>
            <section class="rejilla" aria-label="Héroes">${partida.heroes.map(heroe => luchador(heroe)).join('')}</section>
            <section class="rejilla" aria-label="Enemigos">${partida.enemigos.map(enemigo => luchador(enemigo)).join('')}</section>
        </div>
        <form id="accion" class="panel acciones" aria-label="Acciones disponibles" data-objeto="${accion.objeto?.id ?? ''}">
            <div class="controles-combate">
                <div class="fila"><label>Acción<select name="habilidad">
            ${opciones.map(opcion => `<option value="${opcion.id}" ${opcion.id === accion.id ? 'selected' : ''}
                ${opcion.avisoBloqueo ? 'disabled' : ''}>${escapar(opcion.texto)}${opcion.avisoBloqueo ? ` · ${escapar(opcion.avisoBloqueo)}` : ''}</option>`).join('')}
                </select></label>${elegirObjetivo}<button class="principal" type="submit">Actuar</button></div>
                <p class="orden">
                    <span class="turno-actual">Turno de <strong>${nombre(actor.tipo)}</strong></span>
                    <span>Siguientes: ${nombresDeLosSiguientesTurnos().join(' / ')}</span>
                </p>
            </div>
            <div class="detalle-accion">
                ${accion.objeto ? iconoObjeto(accion.objeto) : ''}
                <small>${escapar(accion.descripcion)}${accion.id === 'robar' ? ` Probabilidad: ${actor.probabilidadRobo} %.` : ''}</small>
            </div>
        </form>${historial()}`;
    juego.querySelectorAll('.escenario .luchador h3').forEach(titulo => observarNombresCombate.observe(titulo));
}

function describir(objeto) {
    const bonos = Object.entries(objeto.bonificaciones).filter(([, valor]) => valor !== 0)
        .map(([clave, valor]) => `${valor > 0 ? '+' : ''}${valor} ${atributos[clave]}`).join(' · ');
    if (!objeto.efecto) return bonos;
    const {tipo, objetivo, cantidad, magia, proteccion} = objeto.efecto;
    const grupo = objetivo === 'ENEMIGOS' ? ' a todos los enemigos' : objetivo === 'ALIADOS' ? ' a todos los aliados vivos' : '';
    if (tipo === 'DANIO') return `${cantidad} de daño mágico${grupo}${objeto.efecto.incendiario ? ' · Puede causar quemaduras' : ''}`;
    if (tipo === 'ANIQUILAR_DRAGON') return 'Derrota al dragón de un solo golpe.';
    if (tipo === 'REVITALIZACION') return `Revive a un héroe caído con el ${cantidad} % de sus PV máximos.`;
    if (tipo === 'PROTECCION') return `${proteccion} % de reducción durante 2 turnos${grupo}`;
    if (tipo === 'CURA_TOTAL') return `Recupera ${cantidad} % de PV máximos`;
    return [cantidad ? `+${cantidad} PV${grupo}` : '', magia ? `+${magia} PC` : ''].filter(Boolean).join(' · ');
}

function articulo(objeto, controles, detalle = '') {
    const calidad = objeto.calidad;
    const requisitos = [objeto.fuerzaMinima && `FUE base ${objeto.fuerzaMinima}`,
        objeto.inteligenciaMinima && `INT base ${objeto.inteligenciaMinima}`,
        objeto.clasesCompatibles?.map(nombre).join(', ')].filter(Boolean).join(' · ');
    return `<article class="tarjeta articulo"><div class="cabecera-objeto">${iconoObjeto(objeto)}<h3>${escapar(nombreObjeto(objeto))}</h3></div>
        ${calidad ? `<small>${objeto.categoria === 'ARMAS' ? 'Calidad: ' : ''}${escapar(nombre(calidad))}${objeto.inicial ? ' · Inicial' : ''}</small>` : ''}
        ${objeto.ranura ? `<small>${ranurasEquipo[objeto.ranura]}${objeto.dosManos ? ' · Dos manos' : ''}</small>` : ''}
        <small>${escapar(describir(objeto))}</small><small>Requisitos · ${requisitos || 'Ninguno'}</small>
        ${objeto.ranura === 'ESCUDO' ? '<small>Requiere un arma de una mano. Defenderse e Interceptar reducen el daño un 65 % con el escudo equipado.</small>' : ''}
        ${objeto.bonoMaximoFuriaMaldita ? `<small>Furia maldita · El daño físico aumenta hasta un ${objeto.bonoMaximoFuriaMaldita} % según los PV perdidos.</small>` : ''}
        ${objeto.habilidad ? `<small>${escapar(objeto.habilidad.nombre)} · ${objeto.habilidad.costePm} PC</small>` : ''}
        ${detalle}<div class="fila">${controles}</div></article>`;
}

function comparacion(objeto, heroe) {
    if (!objeto.ranura) return '';
    const equipado = heroe.equipo[objeto.ranura];
    const diferencias = Object.entries(objeto.bonificaciones).map(([clave, cantidad]) =>
        [clave, cantidad - (equipado?.bonificaciones[clave] ?? 0)]).filter(([, cantidad]) => cantidad)
        .map(([clave, cantidad]) => `${cantidad > 0 ? '+' : ''}${cantidad} ${atributos[clave]}`).join(' · ');
    return `<small>${escapar(equipado ? `Frente a ${equipado.nombre}` : 'Ranura vacía')} · ${diferencias || 'Mismos atributos'}</small>`;
}

function mostrarCampamento() {
    const heroe = partida.heroes.find(jugador => jugador.id === heroeElegido) ?? partida.heroes[0];
    heroeElegido = heroe.id;
    const selector = `<div class="fila"><label>Héroe <select id="heroe">${partida.heroes.map(jugador =>
        `<option value="${jugador.id}" ${jugador.id === heroe.id ? 'selected' : ''}>${nombre(jugador.tipo)}</option>`).join('')}</select></label></div>`;
    const botonEquipar = objeto => {
        const claseCompatible = !objeto.clasesCompatibles?.length || objeto.clasesCompatibles.includes(heroe.tipo);
        const arma = heroe.equipo.ARMAS;
        const escudoCompatible = objeto.ranura !== 'ESCUDO' || (arma && !arma.dosManos);
        const motivos = [];
        if (!claseCompatible) motivos.push('El héroe no puede usar este equipo por su clase.');
        if (!escudoCompatible) motivos.push('El escudo requiere un arma de una mano.');
        if (heroe.base.fuerza < objeto.fuerzaMinima) {
            motivos.push(`FUE base insuficiente (${heroe.base.fuerza}/${objeto.fuerzaMinima}).`);
        }
        if (heroe.base.inteligencia < objeto.inteligenciaMinima) {
            motivos.push(`INT base insuficiente (${heroe.base.inteligencia}/${objeto.inteligenciaMinima}).`);
        }
        const aviso = motivos.length ? motivos.join(' ')
            : objeto.dosManos && heroe.equipo.ESCUDO ? 'El escudo volverá al inventario.' : '';
        return boton('Equipar', 'equipar', objeto.id, motivos.length ? 'disabled' : '')
            + (aviso ? `<small>${escapar(aviso)}</small>` : '');
    };
    let contenido;
    if (panel === 'recompensas') {
        const recompensa = partida.recompensa;
        contenido = `<h2>Ronda ${partida.ronda} superada</h2>
            <p>${partida.oroIlimitado ? '' : `+${recompensa.oro} MO · `}+${recompensa.experiencia} EXP por héroe superviviente</p>
            <p class="ayuda">${recompensa.turnos} acciones · ${recompensa.supervivientes} supervivientes · ${recompensa.consumibles} consumibles usados</p>
            <h3>Botín</h3><div class="rejilla">${partida.botin.map(objeto => articulo(objeto, boton('Recoger', 'recoger', objeto.id))).join('')
                || '<p class="ayuda">No queda botín por recoger.</p>'}</div>`;
    } else if (panel === 'estado') {
        contenido = `<div class="rejilla estado-equipo">${partida.heroes.map(heroe => luchador(heroe, true)).join('')}</div>`;
    } else if (panel === 'inventario') {
        contenido = `<h2>Inventario</h2>${selector}<div class="rejilla">${partida.inventario.map(objeto => {
            let control = boton(`Vender por ${objeto.precioVenta} MO`, 'vender', objeto.id);
            if (objeto.ranura) {
                control = botonEquipar(objeto) + control;
            } else if (objeto.categoria === 'POCIONES') {
                const revitaliza = objeto.efecto.tipo === 'REVITALIZACION';
                const objetivoValido = revitaliza ? heroe.pv === 0 : heroe.pv > 0;
                control = boton(revitaliza ? 'Revivir a este héroe' : 'Usar en este héroe', 'pocion', objeto.id,
                    objetivoValido ? '' : 'disabled') + control;
            }
            return articulo(objeto, control, comparacion(objeto, heroe));
        }).join('') || '<p class="ayuda">El inventario está vacío.</p>'}</div>`;
    } else if (panel === 'equipo') {
        const disponibles = partida.inventario.filter(objeto => objeto.ranura);
        const posiciones = ['CABEZA', 'ARMAS', 'ARMADURAS', 'GUANTES', 'ESCUDO', 'BOTAS'];
        contenido = `<h2>Equipamiento</h2>${selector}<div class="rejilla-equipamiento">${posiciones.map(ranura => `
            <div class="ranura-equipo" data-ranura="${ranura}">
                ${heroe.equipo[ranura] ? articulo(heroe.equipo[ranura], ranura === 'ARMAS' ? '' : boton('Desequipar', 'desequipar', ranura))
                    : `<article class="tarjeta"><h3>${ranurasEquipo[ranura]}</h3><small>${ranura !== 'ESCUDO' ? 'Ranura vacía'
                        : !clasesConEscudo.includes(heroe.tipo) ? 'Esta clase no puede equipar escudo'
                        : heroe.equipo.ARMAS?.dosManos ? 'Ocupada por un arma de dos manos' : 'Ranura vacía'}</small></article>`}
            </div>`).join('')}</div>
            <h3 class="fila">Armas y armaduras del inventario</h3>
            <div class="rejilla">${disponibles.map(objeto => articulo(objeto, botonEquipar(objeto), comparacion(objeto, heroe))).join('')
                || '<p class="ayuda">El inventario no contiene armas ni armaduras.</p>'}</div>`;
    } else if (panel === 'tienda') {
        contenido = `<h2>Tienda</h2>${selector}<div class="pestanas">${categorias.map(apartado =>
            boton(nombre(apartado), 'categoria', apartado, `aria-pressed="${categoria === apartado}"`)).join('')}</div>
            <div class="rejilla">${partida.tienda.filter(({objeto}) => objeto.categoria === categoria).map(({objeto, precio, existencias}) => {
                const disponible = existencias !== 0 && (partida.oroIlimitado || partida.oro >= precio);
                const etiqueta = objeto.id ? 'Recomprar' : 'Comprar';
                return articulo(objeto, boton(`${etiqueta} por ${precio} MO`, objeto.id ? 'recomprar' : 'comprar',
                    objeto.id ?? objeto.codigo, disponible ? '' : 'disabled'),
                    `<small>${objeto.id ? 'Segunda mano · ' : ''}${existencias === 0 ? 'Agotado' : existencias == null ? 'Existencias ilimitadas' : '1 unidad'}</small>` + comparacion(objeto, heroe));
            }).join('')}</div>`;
    } else {
        contenido = `<h2>Subida de nivel</h2>${selector}
            <p>Nivel ${heroe.nivel} · ${heroe.experiencia}/${heroe.experienciaSiguiente} EXP · ${heroe.puntos} puntos disponibles</p>
            <div class="fila">${Object.entries({PV: heroe.base.pvMaximos, PM: heroe.base.pmMaximos, FUE: heroe.base.fuerza,
                DEF: heroe.base.defensa, INT: heroe.base.inteligencia, RES: heroe.base.resistencia, VEL: heroe.base.velocidad,
                DES: heroe.base.destreza}).filter(([, valor]) => valor !== undefined)
                .map(([atributo, valor]) => boton(`${atributo === 'PM' ? 'PC' : atributo} ${valor} · +1 punto`, 'atributo', atributo,
                    heroe.puntos ? '' : 'disabled')).join('')}</div>`;
    }
    juego.innerHTML = `<div class="pestanas">${Object.entries(paneles).map(([clave, etiqueta]) =>
        boton(etiqueta, 'panel', clave, `aria-pressed="${panel === clave}"`)).join('')}</div>
        <section>${contenido}</section><div class="fila">${boton('Siguiente combate', 'avanzar', '', 'class="principal"')}</div>${historial()}`;
}

document.addEventListener('click', async evento => {
    const boton = evento.target.closest('button[data-accion]');
    if (!boton || ocupado) return;
    const {accion, valor} = boton.dataset;
    switch (accion) {
        case 'crear': seleccion = []; dificultadElegida = 'FACIL'; await operar('crear'); break;
        case 'reiniciar':
            if (partida.ronda && !window.confirm('¿Empezar una nueva partida? Se perderá el progreso actual.')) break;
            if (await operar('reiniciar')) { seleccion = []; dificultadElegida = 'FACIL'; mostrar(); }
            break;
        case 'clase':
            if (seleccion.includes(valor)) seleccion = seleccion.filter(clase => clase !== valor);
            else if (seleccion.length < 3) seleccion = [...seleccion, valor];
            else { informar('Quita una clase para elegir otra.'); break; }
            mostrar();
            juego.querySelector(`[data-valor="${valor}"]`)?.focus();
            break;
        case 'iniciar':
            if (await operar('seleccion', { clases: seleccion })) await operar('iniciar', { dificultad: dificultadElegida });
            break;
        case 'panel': panel = valor; mostrar(); break;
        case 'categoria': categoria = valor; mostrar(); break;
        case 'recoger': await operar('recoger', { objeto: valor }); break;
        case 'equipar': await operar('equipar', { actor: heroeElegido, objeto: valor }); break;
        case 'desequipar': await operar('desequipar', { actor: heroeElegido, ranura: valor }); break;
        case 'comprar': await operar('comprar', { codigo: valor }); break;
        case 'recomprar': await operar('comprar', { objeto: valor }); break;
        case 'vender': await operar('vender', { objeto: valor }); break;
        case 'pocion': {
            const objetivo = partida.heroes.find(heroe => heroe.id === heroeElegido);
            const usuario = objetivo.pv > 0 ? objetivo : partida.heroes.find(heroe => heroe.pv > 0);
            await operar('consumir', { actor: usuario.id, objetivo: objetivo.id, objeto: valor });
            break;
        }
        case 'atributo': await operar('atributos', { actor: heroeElegido, atributo: valor, cantidad: 1 }); break;
        case 'avanzar':
            if (partida.botin.length && !window.confirm('¿Avanzar sin recoger el botín restante?')) break;
            await operar('avanzar', { ronda: partida.ronda });
            break;
    }
});

juego.addEventListener('change', evento => {
    if (ocupado) return;
    if (evento.target.id === 'dificultad') {
        dificultadElegida = evento.target.value;
    } else if (evento.target.name === 'habilidad') {
        accionElegida = evento.target.value;
        mostrarCombate();
        juego.querySelector('[name="habilidad"]').focus();
    } else if (evento.target.id === 'heroe') {
        heroeElegido = evento.target.value;
        mostrarCampamento();
        juego.querySelector('#heroe').focus();
    }
});

juego.addEventListener('submit', async evento => {
    if (evento.target.id !== 'accion') return;
    evento.preventDefault();
    const datos = new FormData(evento.target);
    const objeto = evento.target.dataset.objeto;
    await operar(objeto ? 'consumir' : 'accion', {
        turno: partida.turno, actor: partida.orden[0], habilidad: datos.get('habilidad'),
        objetivo: datos.get('objetivo'), objeto: objeto || null
    });
});

async function iniciar() {
    mostrar();
    try {
        perfiles = await api(`${ruta}/clases`);
        const guardada = localStorage.getItem(clave);
        if (guardada) {
            actualizar(await api(`${ruta}/${guardada}`));
            seleccion = partida.heroes.map(heroe => heroe.tipo);
        }
        mostrar();
    } catch (error) {
        if (error.estado === 404) volverAlInicio();
        else informar(error.message);
    }
}

iniciar();
