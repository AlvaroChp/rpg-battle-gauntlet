'use strict';

const juego = document.querySelector('#juego');
const resumen = document.querySelector('#resumen');
const aviso = document.querySelector('#aviso');
const ruta = '/api/partidas';
const clave = 'gauntlet-partida';
const nombres = {
    GUERRERO: 'Guerrero', MAGO: 'Mago', ARQUERO: 'Arquero', SANADOR: 'Sanador', PICARO: 'Pícaro',
    GOBLIN: 'Goblin', ESQUELETO: 'Esqueleto', CHAMAN: 'Chamán', OGRO: 'Ogro',
    CABALLERO_OSCURO: 'Caballero oscuro', DRAGON: 'Dragón',
    ARMAS: 'Armas', ARMADURAS: 'Armaduras', POCIONES: 'Pociones', PERGAMINOS: 'Pergaminos',
    DEBIL: 'Débil', NORMAL: 'Normal', PODEROSO: 'Poderoso', INFERIOR: 'Inferior', SUPERIOR: 'Superior',
    DEFENSA: 'Defensa', GUARDIA: 'Guardia', BENDICION: 'Bendición', EVASION: 'Esquiva',
    RALENTIZACION: 'Ralentización', DEFENSA_DEBILITADA: 'Defensa debilitada',
    GUARDIA_OSEA_ACTIVA: 'Guardia ósea', GUARDIA_OSCURA_ACTIVA: 'Guardia oscura',
    CONTRAATAQUE_LISTO: 'Contraataque preparado', PROTECCION_35: 'Protección del 35 %',
    PROTECCION_50: 'Protección del 50 %', PROTECCION_65: 'Protección del 65 %'
};
const iconos = { GUERRERO: '⚔', MAGO: '✦', ARQUERO: '➶', SANADOR: '✚', PICARO: '◆',
    GOBLIN: '♦', ESQUELETO: '☠', CHAMAN: '✷', OGRO: '⬟', CABALLERO_OSCURO: '♜', DRAGON: '♞' };
const atributos = { fuerza: 'FUE', defensa: 'DEF', inteligencia: 'INT', resistencia: 'RES',
    velocidad: 'VEL', critico: 'Crítico', esquiva: 'Esquiva', potenciaMagica: 'Potencia mágica' };
const categorias = ['ARMAS', 'ARMADURAS', 'POCIONES', 'PERGAMINOS'];
const paneles = { recompensas: 'Recompensas', inventario: 'Inventario', equipo: 'Equipamiento', tienda: 'Tienda', nivel: 'Nivel' };
const nombresId = new Map();
let partida;
let perfiles = [];
let seleccion = [];
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
        actualizar(nueva);
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
    resumen.innerHTML = partida ? `
        ${partida.ronda ? `<span>Ronda ${partida.ronda} de 10</span><span>${partida.oro} MO</span>` : ''}
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
    juego.innerHTML = `<h1>Elige a tus héroes</h1><p class="ayuda">${seleccion.length} de 3 clases seleccionadas</p>
        <div class="rejilla">${perfiles.map(({clase, base}) => `
            <button type="button" class="tarjeta seleccion" data-accion="clase" data-valor="${clase}"
                    aria-pressed="${seleccion.includes(clase)}">
                <span class="icono" aria-hidden="true">${iconos[clase]}</span>
                <strong>${nombre(clase)}</strong>
                <small>${base.pvMaximos} PV · ${base.pmMaximos} PM</small>
                <small>${Object.entries(atributos).filter(([clave]) => clave in base)
                    .map(([clave, etiqueta]) => `${etiqueta} ${base[clave]}`).join(' · ')}</small>
            </button>`).join('')}</div>
        <div class="fila">${boton('Iniciar combate', 'iniciar', '', `class="principal" ${seleccion.length === 3 ? '' : 'disabled'}`)}</div>`;
}

function barra(etiqueta, actual, maximo, clase = '') {
    return `<div class="barra"><span>${actual}/${maximo} ${etiqueta}</span>
        <progress class="${clase}" aria-label="${etiqueta}" value="${actual}" max="${maximo || 1}"></progress></div>`;
}

function luchador(datos) {
    return `<article class="tarjeta ${datos.pv === 0 ? 'caido' : ''} ${partida.orden[0] === datos.id ? 'activo' : ''}">
        <span class="icono" aria-hidden="true">${iconos[datos.tipo]}</span><h3>${nombre(datos.tipo)}</h3>
        ${barra('PV', datos.pv, datos.base.pvMaximos)}${barra('PM', datos.pm, datos.base.pmMaximos, 'magia')}
        ${datos.pv === 0 ? '<small>Fuera de combate</small>' : ''}
        ${Object.entries(datos.estados).map(([estado, turnos]) => `<small>${escapar(nombre(estado))} · ${turnos} turno(s)</small>`).join('')}
    </article>`;
}

function historial() {
    const etiquetas = { ataque_basico: 'Ataque básico', defender: 'Defenderse', bola_fuego: 'Bola de fuego',
        fuego_chaman: 'Bola de fuego', aliento_fuego: 'Aliento de fuego', barrido: 'Ataque de área' };
    const eventos = partida.historial.slice().reverse();
    return `<details class="panel" ${partida.estado === 'COMBATE' ? 'open' : ''}><summary>Historial de combate (${eventos.length})</summary>
        <ol class="registro">${eventos.map(evento => {
            const accion = etiquetas[evento.habilidad] ?? evento.habilidad.replaceAll('_', ' ');
            const resultado = evento.esquivado ? ' · esquivado'
                : evento.danio ? ` · ${evento.danio} de daño${evento.critico ? ' crítico' : ''}`
                : evento.curacion ? ` · +${evento.curacion} PV` : '';
            const magia = evento.magiaRecuperada ? ` · +${evento.magiaRecuperada} PM` : '';
            return `<li>${evento.turno} · ${escapar(nombre(evento.tipoActor))} · ${escapar(accion)}
                · ${escapar(nombre(evento.tipoObjetivo))}${resultado}${magia}</li>`;
        }).join('')}</ol></details>`;
}

function mostrarCombate() {
    const actor = partida.heroes.find(heroe => heroe.id === partida.orden[0]);
    if (!actor) return;
    const consumibles = partida.inventario.filter(objeto => objeto.efecto);
    const opciones = [
        ...partida.habilidades.map(habilidad => ({ ...habilidad, texto: `${habilidad.nombre} · ${habilidad.costePm} PM` })),
        ...consumibles.map(objeto => ({ id: objeto.id, objetivo: objeto.efecto.objetivo, objeto,
            texto: objeto.nombre + (objeto.potencia ? ` · ${nombre(objeto.potencia)}` : ''),
            descripcion: describir(objeto), bloqueado: actor.base.inteligencia < objeto.inteligenciaMinima }))
    ];
    const accion = opciones.find(opcion => opcion.id === accionElegida && !opcion.bloqueado) ?? opciones[0];
    accionElegida = accion.id;
    const objetivos = accion.objetivo === 'ENEMIGO' ? partida.enemigos : accion.objetivo === 'ALIADO' ? partida.heroes : [];
    const elegirObjetivo = objetivos.length ? `
        <label>Objetivo<select name="objetivo" required>${objetivos.filter(objetivo => objetivo.pv > 0)
            .map(objetivo => `<option value="${objetivo.id}">${nombre(objetivo.tipo)} · ${objetivo.pv} PV</option>`).join('')}</select></label>` : '';
    juego.innerHTML = `<h1>Combate</h1><p class="orden">Turno de <strong>${nombre(actor.tipo)}</strong> · ${partida.orden.map(id => nombresId.get(id)).join(' / ')}</p>
        <div class="bandos">
            <section><h2>Héroes</h2><div class="rejilla">${partida.heroes.map(luchador).join('')}</div></section>
            <section><h2>Enemigos</h2><div class="rejilla">${partida.enemigos.map(luchador).join('')}</div></section>
        </div>
        <form id="accion" class="panel acciones" data-objeto="${accion.objeto?.id ?? ''}">
            <h2>Acciones disponibles</h2>
            <div class="fila"><label>Acción<select name="habilidad">
            ${opciones.map(opcion => `<option value="${opcion.id}" ${opcion.id === accion.id ? 'selected' : ''}
                ${opcion.bloqueado ? 'disabled' : ''}>${escapar(opcion.texto)}${opcion.bloqueado ? ' · INT insuficiente' : ''}</option>`).join('')}
            </select></label>${elegirObjetivo}<button class="principal" type="submit">Actuar</button></div>
            <small>${escapar(accion.descripcion)}</small>
        </form>${historial()}`;
}

function describir(objeto) {
    const bonos = Object.entries(objeto.bonificaciones).filter(([, valor]) => valor !== 0)
        .map(([clave, valor]) => `${valor > 0 ? '+' : ''}${valor} ${atributos[clave]}`).join(' · ');
    if (!objeto.efecto) return bonos;
    const {tipo, objetivo, cantidad, magia, proteccion} = objeto.efecto;
    const grupo = objetivo === 'ENEMIGOS' ? ' a todos los enemigos' : objetivo === 'ALIADOS' ? ' a todos los aliados vivos' : '';
    if (tipo === 'DANIO') return `${cantidad} de daño mágico${grupo}`;
    if (tipo === 'PROTECCION') return `${proteccion} % de reducción durante 2 turnos${grupo}`;
    if (tipo === 'CURA_TOTAL') return `Recupera ${cantidad} % de PV máximos${proteccion ? ` · protección del ${proteccion} % durante 2 turnos` : ''}`;
    return [cantidad ? `+${cantidad} PV${grupo}` : '', magia ? `+${magia} PM` : ''].filter(Boolean).join(' · ');
}

function articulo(objeto, controles, detalle = '') {
    const variante = objeto.calidad ?? objeto.potencia;
    const requisitos = [objeto.fuerzaMinima && `FUE ${objeto.fuerzaMinima}`,
        objeto.inteligenciaMinima && `INT ${objeto.inteligenciaMinima}`].filter(Boolean).join(' · ');
    return `<article class="tarjeta articulo"><h3>${escapar(objeto.nombre)}</h3>
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
    let contenido;
    if (panel === 'recompensas') {
        const recompensa = partida.recompensa;
        contenido = `<h2>Ronda ${partida.ronda} superada</h2>
            <p>+${recompensa.oro} MO · +${recompensa.experiencia} EXP por héroe</p>
            <p class="ayuda">${recompensa.turnos} acciones · ${recompensa.supervivientes} supervivientes · ${recompensa.consumibles} consumibles usados</p>
            <h3>Botín</h3><div class="rejilla">${partida.botin.map(objeto => articulo(objeto, boton('Recoger', 'recoger', objeto.id))).join('')
                || '<p class="ayuda">No queda botín por recoger.</p>'}</div>`;
    } else if (panel === 'inventario') {
        contenido = `<h2>Inventario</h2>${selector}<div class="rejilla">${partida.inventario.map(objeto => {
            let control = objeto.inicial ? '' : boton(`Vender por ${objeto.precioVenta} MO`, 'vender', objeto.id);
            if (categorias.slice(0, 2).includes(objeto.categoria)) {
                const valido = heroe.base.fuerza >= objeto.fuerzaMinima && heroe.base.inteligencia >= objeto.inteligenciaMinima;
                control = boton('Equipar', 'equipar', objeto.id, valido ? '' : 'disabled') + control;
            } else if (objeto.categoria === 'POCIONES') {
                control = boton('Usar en este héroe', 'pocion', objeto.id, heroe.pv > 0 ? '' : 'disabled') + control;
            }
            return articulo(objeto, control, comparacion(objeto, heroe));
        }).join('') || '<p class="ayuda">El inventario está vacío.</p>'}</div>`;
    } else if (panel === 'equipo') {
        contenido = `<h2>Equipamiento</h2>${selector}<div class="rejilla">${categorias.slice(0, 2).map(ranura =>
            heroe.equipo[ranura] ? articulo(heroe.equipo[ranura], boton('Desequipar', 'desequipar', ranura))
                : `<article class="tarjeta"><h3>${nombre(ranura)}</h3><small>Ranura vacía</small></article>`).join('')}</div>`;
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
                DEF: heroe.base.defensa, INT: heroe.base.inteligencia, RES: heroe.base.resistencia, VEL: heroe.base.velocidad})
                .map(([atributo, valor]) => boton(`${atributo} ${valor} · +1 punto`, 'atributo', atributo,
                    heroe.puntos ? '' : 'disabled')).join('')}</div>`;
    }
    juego.innerHTML = `<h1>Campamento</h1><div class="pestanas">${Object.entries(paneles).map(([clave, etiqueta]) =>
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
            if (await operar('seleccion', { clases: seleccion })) await operar('iniciar');
            break;
        case 'panel': panel = valor; mostrar(); break;
        case 'categoria': categoria = valor; mostrar(); break;
        case 'recoger': await operar('recoger', { objeto: valor }); break;
        case 'equipar': await operar('equipar', { actor: heroeElegido, objeto: valor }); break;
        case 'desequipar': await operar('desequipar', { actor: heroeElegido, categoria: valor }); break;
        case 'comprar': await operar('comprar', { codigo: valor }); break;
        case 'recomprar': await operar('comprar', { objeto: valor }); break;
        case 'vender': await operar('vender', { objeto: valor }); break;
        case 'pocion': await operar('consumir', { actor: heroeElegido, objetivo: heroeElegido, objeto: valor }); break;
        case 'atributo': await operar('atributos', { actor: heroeElegido, atributo: valor, cantidad: 1 }); break;
        case 'avanzar':
            if (partida.botin.length && !window.confirm('¿Avanzar sin recoger el botín restante?')) break;
            await operar('avanzar', { ronda: partida.ronda });
            break;
    }
});

juego.addEventListener('change', evento => {
    if (evento.target.name === 'habilidad') {
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
