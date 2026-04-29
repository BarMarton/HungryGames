
const mapContainer = document.getElementById('map-wrapper');
const characterBoard = document.getElementById("sub_base");
const leaderboard = document.getElementById('leader');
const popup = document.getElementById('popup');
const prize = document.getElementById('winamount');
const tempNum = 10;
let img_size = document.getElementById('m_image');
const mapContainerElement = document.querySelector('.map');
//debug list
//let characters_debug = ["ayna", "apa", "gyerekek","minőségtelen lacnsika","mákos tészta","MAMA"];

const kepekAdatok = JSON.parse(localStorage.getItem("npcCache") || "[]");
var width
var height
let isDragging = false;
let startX, startY, scrollLeft, scrollTop;
let aliveCount = 0;
let marFogadott = false;

let characters = []
let weapons = [];
const weaponImages = [
    "/pic/weapons/pisztoly.png",
    "/pic/weapons/kard.png",
    "/pic/weapons/ij.png"
];

//const fogadott = localStorage.getItem("fogadott") === "true";

// function fillBase() {
//     for (let i = 0; i < tempNum; i++) {
//         characterBoard.innerHTML += '<div class="character"><img src="../../assets/characters/fish.png"></div>';
//     }
// }

//---------------------------------------------------Websocket_is_that_easy-------------------------
const client = new StompJs.Client({
    webSocketFactory: () => new SockJS(window.location.origin + '/ws'),
    onConnect: () => {
        console.log("Sesxy");

        client.subscribe('/topic/game.status', msg => {
            const status = JSON.parse(msg.body);
            if (status.status === 'FINISHED') {
                activateWinPopup([status.winnerNpcName]);
                loadCurrentUser();
            } else if (status.status === 'IN_PROGRESS') {
                events.showEvent("The game has started");
            } else if (status.status === 'BETTING') {
                console.log("FOGADÁS INDULT - Átirányítás..."); 
                localStorage.removeItem("fogadott");
                window.location.replace('/fogadas')
            }
        });

        client.subscribe('/topic/game.state', msg => {
            width = img_size.clientWidth;
            height = img_size.clientHeight;
            const state = JSON.parse(msg.body);
            aliveCount = state.npcs.filter(n => n.alive).length;
            
            state.npcs.forEach(npc => {
                if (npc.alive) {
                    const mappedX = (npc.x / 100) * width;
                    const mappedY = (npc.y / 100) * height;
                    
                    updateCharacterOnMap(npc.id, "pic/characters/"+ npc.id +".jpg", mappedX, mappedY);
                } else {
                    remove_character(npc.id);
                }
            });

            if (state.weapons) {

            const currentWeaponIds = state.weapons.map(w => `map-weapon-${w.id}`);

            weapons.forEach(w => {
                if (!currentWeaponIds.includes(w.id)) {
                    w.remove();
                }
            });

            weapons = [];

            state.weapons.forEach(w => {
                const mappedX = (w.x / 100) * width;
                const mappedY = (w.y / 100) * height;

                const weaponDiv = updateWeaponOnMap(
                    w.id,
                    "pic/money.jpg",
                    mappedX,
                    mappedY
                );

                weapons.push(weaponDiv);
            });
        }
        });

        client.subscribe('/topic/game.events', msg => {
            const eventPayload = JSON.parse(msg.body);
            
            eventPayload.events.forEach(e => {
                if (e.type === 'DEATH') {
                    if (aliveCount > 1) {
                            playSoundSafe("/audio/kill.mp3");
                    }
                    events.showEvent(`💀 ${e.deadNpcName} elesett!`);
                    remove_character(e.deadNpcId);
                } else if (e.type === 'WEAPON_PICKUP') {
                    playSoundSafe("/audio/pickup.mp3");
                    events.showEvent(`⚔️ ${e.attackerName} fegyvert talált! (+${e.damage} DMG)`);
                }
            });
        });
    }
});

function playSound(src) {
    console.log("🎵 TRY PLAY:", src);

    const audio = new Audio();

    audio.addEventListener("loadstart", () => console.log("▶ loadstart"));
    audio.addEventListener("canplay", () => console.log("▶ canplay"));
    audio.addEventListener("canplaythrough", () => console.log("▶ canplaythrough"));
    audio.addEventListener("error", (e) => {
        console.error("❌ AUDIO ERROR:", src, e);
        console.log("audio.error code:", audio.error);
    });

    audio.src = src;
    audio.volume = 1.0;

    audio.play()
        .then(() => console.log("✅ PLAY SUCCESS"))
        .catch(err => console.error("❌ PLAY BLOCKED:", err));
}

let lastSoundTime = 0;

function playSoundSafe(src) {
    const now = Date.now();
    if (now - lastSoundTime < 200) return; 

    lastSoundTime = now;
    playSound(src);
}

async function loadCurrentUser() {
    const userId = getCookie("userId");

    if (!userId) {
        console.error("Nincs userId cookie!");
        return;
    }

    const response = await fetch(`/api/users/${userId}`);

    if (response.ok) {
        const user = await response.json();

        const penzElem = document.querySelector(".penzSzoveg");
        if (penzElem) {
            penzElem.textContent = user.balance + " Ft";
        }

        console.log("UPDATED BALANCE:", user.balance);
    } else {
        console.error("Nem sikerült lekérni a user adatokat");
    }
}

async function lekerFogadasAllapot() {
    const userId = getCookie("userId");
    if (!userId) return;

    try {
        const response = await fetch(`/api/bets/has-bet?userId=${userId}`);
        if (response.ok) {
            marFogadott = await response.json(); 
            console.log("Fogadás állapota szerverről:", marFogadott);
        }
    } catch (err) {
        console.error("Hiba a fogadás állapotának lekérdezésekor", err);
    }
}

async function getMyWinnings() {
    const userId = getCookie("userId");

    const gameRes = await fetch('/api/games/current');
    const game = await gameRes.json();

    const betsRes = await fetch(`/api/bets/user/${userId}`);
    const bets = await betsRes.json();

    console.log("MY BETS:", bets);


    const myBet = bets.find(b => b.gameId === game.id && b.settled);

    if (!myBet) return 0;

    console.log("MY WIN:", myBet.payout);

    return myBet.payout || 0;
}

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}
//---------------------------------------------------Karakter mozgási szimuláció----------------------------------------
// az inspectorhoz hozzáadni a karaktert
function add_character_board(id, imgSrc) {
    const characterLooker = document.createElement('div');
    characterLooker.id = `character-char-${id}`;
    characterLooker.className = 'character';
    characterLooker.innerHTML = `<img src="${imgSrc}" alt="char">`;

    characterBoard.appendChild(characterLooker);

    return characterLooker;
}

//karakter törlése
function remove_character(id) {
    const index = characters.findIndex(charArr => charArr[1].id === `character-char-${id}`);
    
    if (index !== -1) {
        characters[index][0].remove();
        characters[index][1].remove();
        characters.splice(index, 1);
    }
}


mapContainerElement.addEventListener('mousedown', (e) => {
    isDragging = true;
    mapContainerElement.style.cursor = 'grabbing';
    startX = e.pageX - mapContainerElement.offsetLeft;
    startY = e.pageY - mapContainerElement.offsetTop;
    scrollLeft = mapContainerElement.scrollLeft;
    scrollTop = mapContainerElement.scrollTop;
});

mapContainerElement.addEventListener('mouseleave', () => {
    isDragging = false;
    mapContainerElement.style.cursor = 'grab';
});

mapContainerElement.addEventListener('mouseup', () => {
    isDragging = false;
    mapContainerElement.style.cursor = 'grab';
});

mapContainerElement.addEventListener('mousemove', (e) => {
    if (!isDragging) return;
    e.preventDefault();
    const x = e.pageX - mapContainerElement.offsetLeft;
    const y = e.pageY - mapContainerElement.offsetTop;
    const moveX = x - startX; 
    const moveY = y - startY;
    mapContainerElement.scrollLeft = scrollLeft - moveX;
    mapContainerElement.scrollTop = scrollTop - moveY;
});

function add_character_sim_face(id, imgSrc) {
    charDiv = document.createElement('div');
    charDiv.id = `map-char-${id}`;
    charDiv.className = 'map-character';
    charDiv.innerHTML = `<img src="${imgSrc}" alt="char">`;
    mapContainer.appendChild(charDiv);

    return charDiv
}

// karakterek mozgásának frissítése.. ide jön majd a kapott koordináta
function updateCharacterOnMap(id, imgSrc, x, y) {
    let { normX, normY } = normalizeToImage(x,y)

    let charDiv = document.getElementById(`map-char-${id}`);

    if (!charDiv) {
    charDiv = add_character_sim_face(id, imgSrc);
    characters.push([charDiv, add_character_board(id, imgSrc)]);
    }

    charDiv.style.left = `${normX * 100}%`;
    charDiv.style.top = `${normY * 100}%`;
}

//normalizóció
function normalizeToImage(rawX, rawY) {
    const normX = Math.max(0, Math.min(1, rawX / width));
    const normY = Math.max(0, Math.min(1, rawY / height));
    return { normX, normY };
}

function add_weapon(id) {

    const imgSrc = weaponImages[id % weaponImages.length];

    const weaponDiv = document.createElement('div');
    weaponDiv.id = `map-weapon-${id}`;
    weaponDiv.className = 'map-weapon';
    weaponDiv.innerHTML = `<img src="${imgSrc}" alt="weapon">`;

    mapContainer.appendChild(weaponDiv);
    return weaponDiv;
}

function updateWeaponOnMap(id, imgSrc, x, y) {
    let { normX, normY } = normalizeToImage(x, y);

    let weaponDiv = document.getElementById(`map-weapon-${id}`);

    if (!weaponDiv) {
        weaponDiv = add_weapon(id);
        weapons.push(weaponDiv);
    }

    weaponDiv.style.left = `${normX * 100}%`;
    weaponDiv.style.top = `${normY * 100}%`;

    return weaponDiv;
}

function remove_weapon(id) {
    const el = document.getElementById(`map-weapon-${id}`);
    if (el) {
        el.remove();
    }

    const index = weapons.findIndex(w => w.id === `map-weapon-${id}`);
    if (index !== -1) {
        weapons.splice(index, 1);
    }
}
//-----------------------------------------Win management--------------------------------------------------
function writePrize(prizeWon) {
    console.log("PRIZE RECEIVED:", prizeWon);
    if (prizeWon <= 0) {
        prize.innerHTML = `<p>You have won NOTHING!!!</P>`;
    } else {
        prize.innerHTML = `<p>You have won: ${prizeWon} Ft</P>`;
    }
}

function getPicIdByName(name) {
    const npc = kepekAdatok.find(n => n.cim === name);
    return npc ? npc.picId : 1;
}

function listWinners(character_list) {

    for (let i = 0; i < character_list.length; i++) {

        const name = character_list[i];
        const picId = getPicIdByName(name);

        const characterOnLeaderboard = document.createElement('div');
        characterOnLeaderboard.classList = marFogadott 
            ? 'leader_character' 
            : 'leader_character spectator';

        characterOnLeaderboard.innerHTML = `
            <div class="winner-left">
                <p>${name}</p>
            </div>

            <div class="winner-right">
                <img src="/pic/characters/${picId}.jpg" />
            </div>
        `;

        leaderboard.appendChild(characterOnLeaderboard);
    }
}

async function activateWinPopup(character_list){

    const winnings = await getMyWinnings();

    writePrize(winnings);

    popup.style = "visibility: visible";
    document.querySelectorAll('.slot').forEach((el, i) => {
        el.style.animationDelay = (i * 0.15 + Math.random() * 0.2) + "s";
    });
    listWinners(character_list);

    const img = document.getElementById("resultImage");

    if (marFogadott) {
        if (winnings > 0) {
            playSound("/audio/win.mp3");
            img.src = "/pic/winner.png";
        } else {
            playSound("/audio/lose.mp3");
            img.src = "/pic/loser.png";
        }

        img.style.display = "block";
    } else {
        console.log("Csak néző vagy");
        img.style.display = "none";
    }
}
//------------------------------------------------------------------------------------------------------

class EventManager {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.queue = [];
        this.isShowing = false;
    }

    showEvent(message) {
        this.queue.push(message);
        if (!this.isShowing) {
            this.processNext();
        }
    }

    processNext() {
        if (this.queue.length === 0) {
            this.isShowing = false;
            return;
        }

        this.isShowing = true;
        const msg = this.queue.shift();

        const eventEl = document.createElement('div');
        eventEl.className = 'event-message';
        eventEl.innerText = msg;
        this.container.appendChild(eventEl);

        setTimeout(() => eventEl.classList.add('show'), 10);

        setTimeout(() => {
            eventEl.classList.remove('show');

            setTimeout(() => {
                eventEl.remove();
                this.processNext(); 
            }, 500); 
            
        }, 2000);
    }
}

const events = new EventManager('event-container');


async function main() {
    width = img_size.clientWidth;
    height = img_size.clientHeight;

    fetch("/audio/win.mp3")
      .then(r => console.log("AUDIO STATUS:", r.status, r.headers.get("content-type")))
      .catch(e => console.error("FETCH ERROR:", e));

    loadCurrentUser();
    await lekerFogadasAllapot();
    client.activate();
}

window.addEventListener('load', () => {
    main();
});
