'use strict';

const juego = document.querySelector('#juego');
const resumen = document.querySelector('#resumen');
const aviso = document.querySelector('#aviso');
const ruta = '/api/partidas';
const clave = 'gauntlet-partida';
const nombres = {
    GUERRERO: 'Guerrero', MAGO: 'Mago', ARQUERO: 'Arquera', SANADOR: 'Sanadora', PICARO: 'Pícaro',
    GOBLIN: 'Goblin', ESQUELETO: 'Esqueleto', CHAMAN: 'Chamán', OGRO: 'Ogro',
    CABALLERO_OSCURO: 'Caballero oscuro', DRAGON: 'Dragón',
    ARMAS: 'Armas', ARMADURAS: 'Armaduras', POCIONES: 'Pociones', PERGAMINOS: 'Pergaminos',
    DEBIL: 'Débil', NORMAL: 'Normal', PODEROSO: 'Poderoso', INFERIOR: 'Desgastada', SUPERIOR: 'Superior',
    DEFENSA: 'Defensa', GUARDIA: 'Guardia', BENDICION: 'Bendición', EVASION: 'Esquiva',
    RALENTIZACION: 'Ralentización', DEFENSA_DEBILITADA: 'Defensa debilitada',
    GUARDIA_OSEA_ACTIVA: 'Guardia ósea', GUARDIA_OSCURA_ACTIVA: 'Guardia oscura',
    CONTRAATAQUE_LISTO: 'Contraataque preparado', PROTECCION_35: 'Protección del 35 %',
    PROTECCION_50: 'Protección del 50 %', PROTECCION_65: 'Protección del 65 %'
};
const iconos = { GUERRERO: '⚔', MAGO: '✦', ARQUERO: '➶', SANADOR: '✚', PICARO: '◆',
    GOBLIN: '♦', ESQUELETO: '☠', CHAMAN: '✷', OGRO: '⬟', CABALLERO_OSCURO: '♜', DRAGON: '♞' };
const spritesCombate = {
    GUERRERO: 'guerrero.png', MAGO: 'mago.png', ARQUERO: 'arquero.png', SANADOR: 'sanador.png',
    PICARO: 'picaro.png',
    GOBLIN: 'goblin.png', ESQUELETO: 'esqueleto.png', CHAMAN: 'chaman.png', OGRO: 'ogro.png',
    CABALLERO_OSCURO: 'caballero_oscuro.png', DRAGON: 'dragon.png'
};
const biografias = {
    GUERRERO: {
        titulo: 'Therakos — Guerrero',
        parrafos: [
            'Antiguo capitán de los Caballeros de la Luna Quebrada, Therakos renegó del culto tras descubrir las verdaderas intenciones del Padre Fundador. Acusado de herejía, se vio obligado a acabar con la vida de su mejor amigo en defensa propia.',
            'Desde entonces vive como mercenario, ocultando bajo su armadura los tatuajes que delatan su pasado como miembro de la orden. Ofrece su espada a cualquiera que pueda pagar por sus servicios, pero se niega a servir a tirano alguno.'
        ]
    },
    MAGO: {
        titulo: 'Vaelthar — Mago',
        parrafos: [
            'Misterioso erudito cuyo dominio de la magia supera al de los más prestigiosos estudiosos. Posee un conocimiento extraordinario de la desaparecida civilización Kryptoi, sus costumbres y sus artes arcanas.',
            'Lo que nadie sabe es que Vaelthar es un Kryptoi inmortal que ha sobrevivido al Cataclismo. Ocultando su verdadera naturaleza, lleva siglos recorriendo el mundo bajo distintas identidades, custodiando secretos de una civilización que todos creen extinta.'
        ]
    },
    ARQUERO: {
        titulo: 'Lyra — Arquera',
        parrafos: [
            'Criada en una pequeña comunidad de cazadores, Lyra aprendió desde temprana edad a orientarse en los bosques, rastrear animales y manejar el arco. Su extraordinaria puntería le valió un puesto entre los exploradores encargados de proteger las fronteras de su territorio.',
            'Tras años al servicio de su pueblo, decidió abandonar su hogar y recorrer el mundo, ganándose la vida como exploradora y mercenaria. Su conocimiento del terreno y su capacidad para anticiparse al peligro la convierten en una valiosa compañera de viaje.'
        ]
    },
    SANADOR: {
        titulo: 'Dabria — Sanadora',
        parrafos: [
            'Originaria de Kael-Seriv, Dabria es una eminente estudiosa del Eco, especializada en las artes de la restauración y la creación de barreras energéticas. Su profundo conocimiento de la antigua civilización Kryptoi la llevó a investigar los acontecimientos que desencadenaron el Cataclismo.',
            'Tras descubrir una verdad inquietante en unos documentos prohibidos, decidió abandonar la capital. Desde entonces recorre el mundo en busca de respuestas, empleando su dominio del Eco para proteger y sanar a sus compañeros.'
        ]
    },
    PICARO: {
        titulo: 'Lykos — Pícaro',
        parrafos: [
            'Huérfano desde temprana edad, Lykos creció entre los callejones de Vhaldris, una próspera ciudad comercial donde la opulencia de la nobleza contrasta con la miseria de los barrios bajos. Fue acogido por el gremio de ladrones, cuyos miembros le dieron el nombre por el que se le conoce. Ni él mismo conoce su verdadera identidad ni sus orígenes.',
            'Tras abandonar el gremio, comenzó a recorrer el mundo como mercenario. Aunque desconfía de las grandes promesas y los ideales heroicos, posee su propio código de honor y jamás traiciona a quienes considera sus compañeros.'
        ]
    }
};
const atributos = { fuerza: 'FUE', defensa: 'DEF', inteligencia: 'INT', resistencia: 'RES',
    velocidad: 'VEL', destreza: 'DES', critico: 'Crítico', esquiva: 'Esquiva', potenciaMagica: 'Potencia mágica' };
const categorias = ['ARMAS', 'ARMADURAS', 'POCIONES', 'PERGAMINOS'];
const paneles = { recompensas: 'Recompensas', estado: 'Estado', inventario: 'Inventario', equipo: 'Equipamiento', tienda: 'Tienda', nivel: 'Nivel' };
const dificultades = { FACIL: 'Fácil', DIFICIL: 'Difícil', EXPERTO: 'Experto', PRUEBAS: 'Pruebas' };
const nombresId = new Map();
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
        const error = new Error(contenido.mensaje ?? 'No se pudo completar la petición');
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
    partida.heroes.concat(partida.enemigos).forEach(combatiente => nombresId.set(combatiente.id, nombre(combatiente.tipo)));
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

async function reproducirAcciones(eventos) {
    const figuras = new Map([...juego.querySelectorAll('[data-combatiente]')].map(luchador =>
        [luchador.dataset.combatiente, luchador.querySelector('.figura-combate')]));
    const acciones = [];
    for (const evento of eventos) {
        const anterior = acciones.at(-1);
        if (anterior?.turno === evento.turno && anterior.actor === evento.actor) {
            anterior.eventos.push(evento);
        } else {
            acciones.push({ turno: evento.turno, actor: evento.actor, eventos: [evento] });
        }
    }
    for (const accion of acciones) {
        await animarFiguras([figuras.get(accion.actor)], 'accion-en-curso');
        const objetivos = new Set(accion.eventos.filter(evento => evento.danio > 0)
            .map(evento => figuras.get(evento.objetivo)));
        await animarFiguras(objetivos, 'golpe-recibido');
    }
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
        ${partida.ronda ? `<span>${dificultades[partida.dificultad] ?? dificultades.FACIL}</span><span>Ronda ${partida.ronda} de 10</span><span>${partida.oro} MO</span>` : ''}
        ${boton('Reiniciar', 'reiniciar')}` : '';
    if (!partida) {
        juego.innerHTML = `<section class="inicio">
            <div class="insignia" aria-hidden="true">⚔</div><h1>RPG Battle Gauntlet</h1>
            <p>Reúne a tres héroes, supera diez combates y derrota al dragón.</p>
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
            <p>${victoria ? 'El dragón ha caído. Has superado las diez rondas.' : `Tu equipo cayó en la ronda ${partida.ronda}.`}</p>
            ${boton('Volver a jugar', 'reiniciar', '', 'class="principal"')}
        </section>${historial()}`;
    }
}

function mostrarSeleccion() {
    const modos = partida.dificultadesDisponibles ?? (partida.dificultad ? ['FACIL', 'DIFICIL', 'EXPERTO'] : ['FACIL']);
    juego.innerHTML = `<h1>Elige a tus héroes</h1><p class="ayuda">${seleccion.length} de 3 clases seleccionadas</p>
        <div class="rejilla seleccion-heroes">${perfiles.map(({clase, base}) => {
            const { titulo, parrafos } = biografias[clase];
            return `<article class="tarjeta ficha-seleccion">
                <div class="retrato-seleccion"><img src="imagenes/${spritesCombate[clase]}" alt=""></div>
                <h2>${escapar(titulo)}</h2>
                <small>${base.pvMaximos} PV · ${base.pmMaximos} PM</small>
                <small>${Object.entries(atributos).filter(([clave]) => clave in base && (clave !== 'critico' || !('destreza' in base)))
                    .map(([clave, etiqueta]) => `${etiqueta} ${base[clave]}`).join(' · ')}</small>
                <div class="biografia">${parrafos.map(parrafo => `<p>${escapar(parrafo)}</p>`).join('')}</div>
                ${boton('Elegir', 'clase', clase, `aria-label="Elegir a ${escapar(titulo)}" aria-pressed="${seleccion.includes(clase)}"`)}
            </article>`;
        }).join('')}</div>
        <div class="fila"><label>Dificultad <select id="dificultad">${modos.map(codigo =>
            `<option value="${codigo}" ${codigo === dificultadElegida ? 'selected' : ''}>${dificultades[codigo]}</option>`).join('')}</select></label>
            ${boton('Iniciar combate', 'iniciar', '', `class="principal" ${seleccion.length === 3 ? '' : 'disabled'}`)}</div>`;
}

function barra(etiqueta, actual, maximo, clase = '') {
    return `<div class="barra"><span>${actual}/${maximo} ${etiqueta}</span>
        <progress class="${clase}" aria-label="${etiqueta}" value="${actual}" max="${maximo || 1}"></progress></div>`;
}

function luchador(datos, enCampamento = false) {
    const sprite = spritesCombate[datos.tipo];
    const imagen = sprite
        ? `<img class="sprite-combate" src="imagenes/${sprite}" alt="">`
        : `<span class="icono" aria-hidden="true">${iconos[datos.tipo]}</span>`;
    const estados = Object.entries(datos.estados).map(([estado, turnos]) =>
        `<small>${escapar(nombre(estado))} · ${turnos} ${turnos === 1 ? 'turno' : 'turnos'}</small>`).join('');
    return `<article class="luchador ${datos.pv === 0 ? 'caido' : ''}" data-tipo="${datos.tipo}" data-combatiente="${datos.id}">
        <div class="figura-combate">
            <div class="retrato-combate">${imagen}${estados ? `<div class="estados-combate">${estados}</div>` : ''}</div>
        </div>
        <div class="tarjeta ${!enCampamento && partida.orden[0] === datos.id ? 'activo' : ''}">
            <h3><span>${nombre(datos.tipo)}</span></h3>
            ${enCampamento ? `<small>Nivel ${datos.nivel}</small>` : ''}
            ${barra('PV', datos.pv, datos.base.pvMaximos)}${barra('PM', datos.pm, datos.base.pmMaximos, 'magia')}
            ${enCampamento ? `<small>${datos.pv > 0 ? 'En pie' : 'Fuera de combate'}</small>` : '<small class="aviso-caida">Fuera de combate</small>'}
            ${enCampamento ? `<dl class="equipo-estado">
                ${datos.base.destreza === undefined ? '' : `<dt>Destreza</dt><dd>${datos.base.destreza}</dd>`}
                <dt>Arma</dt><dd>${escapar(datos.equipo.ARMAS?.nombre ?? 'Sin equipar')}</dd>
                <dt>Armadura</dt><dd>${escapar(datos.equipo.ARMADURAS?.nombre ?? 'Sin equipar')}</dd>
            </dl>` : ''}
        </div>
    </article>`;
}

const nombrePergaminoDragon = 'Pergamino de Dragón';
const nombrePocionDorada = 'Poción dorada';

function nombreObjeto(objeto) {
    if (objeto.codigo === 'requiem_dragon') return nombrePergaminoDragon;
    if (objeto.codigo === 'revitalizacion') return nombrePocionDorada;
    return objeto.nombre;
}

function historial() {
    const etiquetas = { ataque_basico: 'Ataque básico', defender: 'Defenderse', bola_fuego: 'Bola de fuego',
        fuego_chaman: 'Bola de fuego', aliento_fuego: 'Aliento de fuego', barrido: 'Ataque de área', robar: 'Robar',
        'Réquiem del Dragón': nombrePergaminoDragon, 'Poción de revitalización': nombrePocionDorada };
    const eventos = partida.historial.slice().reverse();
    return `<details class="panel" ${partida.estado === 'COMBATE' ? 'open' : ''}><summary>Historial de combate (${eventos.length})</summary>
        <ol class="registro">${eventos.map(evento => {
            const accion = etiquetas[evento.habilidad] ?? evento.habilidad.replaceAll('_', ' ');
            const resultado = evento.habilidad === 'robar'
                ? (evento.objetoRobado ? ` · ha robado ${escapar(nombreObjeto(evento.objetoRobado))}` : ' · intento fallido')
                : evento.esquivado ? ' · esquivado'
                : evento.danio ? ` · ${evento.danio} de daño${evento.critico ? ' crítico' : ''}`
                : evento.curacion ? ` · +${evento.curacion} PV` : '';
            const magia = evento.magiaRecuperada ? ` · +${evento.magiaRecuperada} PM` : '';
            return `<li>${evento.turno} · ${escapar(nombre(evento.tipoActor))} · ${escapar(accion)}
                · ${escapar(nombre(evento.tipoObjetivo))}${resultado}${magia}</li>`;
        }).join('')}</ol></details>`;
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
        ...partida.habilidades.map(habilidad => ({ ...habilidad, texto: `${habilidad.nombre} · ${habilidad.costePm} PM`,
            avisoBloqueo: habilidad.id === 'robar' && !objetivosRobo.size ? 'Sin objetivos con botín' : '' })),
        ...consumibles.map(objeto => {
            const { tipo, objetivo } = objeto.efecto;
            const avisoBloqueo = actor.base.inteligencia < objeto.inteligenciaMinima ? 'INT insuficiente'
                : tipo === 'REVITALIZACION' && !aliadosCaidos.length ? 'Sin aliados caídos'
                : tipo === 'ANIQUILAR_DRAGON' && !dragonesVivos.length ? 'Requiere al dragón' : '';
            return { id: objeto.id, objetivo, objeto, avisoBloqueo,
                texto: nombreObjeto(objeto) + (objeto.potencia ? ` · ${nombre(objeto.potencia)}` : ''), descripcion: describir(objeto) };
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
    juego.innerHTML = `<p class="orden">Turno de <strong>${nombre(actor.tipo)}</strong> · ${partida.orden.map(id => nombresId.get(id)).join(' / ')}</p>
        <div class="bandos escenario">
            <section class="rejilla" aria-label="Héroes">${partida.heroes.map(heroe => luchador(heroe)).join('')}</section>
            <section class="rejilla" aria-label="Enemigos">${partida.enemigos.map(enemigo => luchador(enemigo)).join('')}</section>
        </div>
        <form id="accion" class="panel acciones" data-objeto="${accion.objeto?.id ?? ''}">
            <h2>Acciones disponibles</h2>
            <div class="fila"><label>Acción<select name="habilidad">
            ${opciones.map(opcion => `<option value="${opcion.id}" ${opcion.id === accion.id ? 'selected' : ''}
                ${opcion.avisoBloqueo ? 'disabled' : ''}>${escapar(opcion.texto)}${opcion.avisoBloqueo ? ` · ${escapar(opcion.avisoBloqueo)}` : ''}</option>`).join('')}
            </select></label>${elegirObjetivo}<button class="principal" type="submit">Actuar</button></div>
            <small>${escapar(accion.descripcion)}${accion.id === 'robar' ? ` Probabilidad: ${actor.probabilidadRobo} %.` : ''}</small>
        </form>${historial()}`;
    juego.querySelectorAll('.escenario .luchador h3').forEach(titulo => observarNombresCombate.observe(titulo));
}

function describir(objeto) {
    const bonos = Object.entries(objeto.bonificaciones).filter(([, valor]) => valor !== 0)
        .map(([clave, valor]) => `${valor > 0 ? '+' : ''}${valor} ${atributos[clave]}`).join(' · ');
    if (!objeto.efecto) return bonos;
    const {tipo, objetivo, cantidad, magia, proteccion} = objeto.efecto;
    const grupo = objetivo === 'ENEMIGOS' ? ' a todos los enemigos' : objetivo === 'ALIADOS' ? ' a todos los aliados vivos' : '';
    if (tipo === 'DANIO') return `${cantidad} de daño mágico${grupo}`;
    if (tipo === 'ANIQUILAR_DRAGON') return 'Derrota al dragón de un solo golpe.';
    if (tipo === 'REVITALIZACION') return `Revive a un héroe caído con el ${cantidad} % de sus PV máximos.`;
    if (tipo === 'PROTECCION') return `${proteccion} % de reducción durante 2 turnos${grupo}`;
    if (tipo === 'CURA_TOTAL') return `Recupera ${cantidad} % de PV máximos${proteccion ? ` · protección del ${proteccion} % durante 2 turnos` : ''}`;
    return [cantidad ? `+${cantidad} PV${grupo}` : '', magia ? `+${magia} PM` : ''].filter(Boolean).join(' · ');
}

function articulo(objeto, controles, detalle = '') {
    const variante = objeto.calidad ?? objeto.potencia;
    const requisitos = [objeto.fuerzaMinima && `FUE ${objeto.fuerzaMinima}`,
        objeto.inteligenciaMinima && `INT ${objeto.inteligenciaMinima}`,
        objeto.clasesCompatibles?.map(nombre).join(', ')].filter(Boolean).join(' · ');
    return `<article class="tarjeta articulo"><h3>${escapar(nombreObjeto(objeto))}</h3>
        ${variante ? `<small>${escapar(nombre(variante))}${objeto.inicial ? ' · Inicial' : ''}</small>` : ''}
        <small>${escapar(describir(objeto))}</small><small>Requisitos · ${requisitos || 'Ninguno'}</small>
        ${objeto.habilidad ? `<small>${escapar(objeto.habilidad.nombre)} · ${objeto.habilidad.costePm} PM</small>` : ''}
        ${detalle}<div class="fila">${controles}</div></article>`;
}

function comparacion(objeto, heroe) {
    const equipado = heroe.equipo[objeto.categoria];
    if (!categorias.slice(0, 2).includes(objeto.categoria)) return '';
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
        const valido = claseCompatible && heroe.base.fuerza >= objeto.fuerzaMinima && heroe.base.inteligencia >= objeto.inteligenciaMinima;
        return boton('Equipar', 'equipar', objeto.id, valido ? '' : 'disabled');
    };
    let contenido;
    if (panel === 'recompensas') {
        const recompensa = partida.recompensa;
        contenido = `<h2>Ronda ${partida.ronda} superada</h2>
            <p>+${recompensa.oro} MO · +${recompensa.experiencia} EXP por héroe</p>
            <p class="ayuda">${recompensa.turnos} acciones · ${recompensa.supervivientes} supervivientes · ${recompensa.consumibles} consumibles usados</p>
            <h3>Botín</h3><div class="rejilla">${partida.botin.map(objeto => articulo(objeto, boton('Recoger', 'recoger', objeto.id))).join('')
                || '<p class="ayuda">No queda botín por recoger.</p>'}</div>`;
    } else if (panel === 'estado') {
        contenido = `<div class="rejilla estado-equipo">${partida.heroes.map(heroe => luchador(heroe, true)).join('')}</div>`;
    } else if (panel === 'inventario') {
        contenido = `<h2>Inventario</h2>${selector}<div class="rejilla">${partida.inventario.map(objeto => {
            let control = boton(`Vender por ${objeto.precioVenta} MO`, 'vender', objeto.id);
            if (categorias.slice(0, 2).includes(objeto.categoria)) {
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
        const disponibles = partida.inventario.filter(objeto => categorias.slice(0, 2).includes(objeto.categoria));
        contenido = `<h2>Equipamiento</h2>${selector}<div class="rejilla">${categorias.slice(0, 2).map(ranura =>
            heroe.equipo[ranura] ? articulo(heroe.equipo[ranura], ranura === 'ARMAS' ? '' : boton('Desequipar', 'desequipar', ranura))
                : `<article class="tarjeta"><h3>${nombre(ranura)}</h3><small>Ranura vacía</small></article>`).join('')}</div>
            <h3 class="fila">Armas y armaduras del inventario</h3>
            <div class="rejilla">${disponibles.map(objeto => articulo(objeto, botonEquipar(objeto), comparacion(objeto, heroe))).join('')
                || '<p class="ayuda">El inventario no contiene armas ni armaduras.</p>'}</div>`;
    } else if (panel === 'tienda') {
        contenido = `<h2>Tienda</h2>${selector}<div class="pestanas">${categorias.map(apartado =>
            boton(nombre(apartado), 'categoria', apartado, `aria-pressed="${categoria === apartado}"`)).join('')}</div>
            <div class="rejilla">${partida.tienda.filter(({objeto}) => objeto.categoria === categoria).map(({objeto, precio, existencias}) => {
                const disponible = existencias !== 0 && partida.oro >= precio;
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
                .map(([atributo, valor]) => boton(`${atributo} ${valor} · +1 punto`, 'atributo', atributo,
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
        case 'crear': seleccion = []; await operar('crear'); break;
        case 'reiniciar':
            if (partida.ronda && !window.confirm('¿Empezar una nueva partida? Se perderá el progreso actual.')) break;
            if (await operar('reiniciar')) { seleccion = []; mostrar(); }
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
        case 'desequipar': await operar('desequipar', { actor: heroeElegido, categoria: valor }); break;
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
