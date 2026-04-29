let marNovekedett = false;
let marginNovelve = false;
let marFogadott = false;
let aktualisKivalasztott = null;
let currentGameId = null;

// let kepekAdatok = [
//     { nev: "pic/characters/1.jpg", cim: "Gigachad", szoveg: "Leírás" },
//     { nev: "pic/characters/2.jpg", cim: "Shrek", szoveg: "Leírás." },
//     { nev: "pic/characters/3.jpg", cim: "Arató András", szoveg: "Leírás" },
//     { nev: "pic/characters/4.jpg", cim: "Homer Simpson", szoveg: "Leírás" },
//     { nev: "pic/characters/5.jpg", cim: "Mike Wazowski", szoveg: "Leírás" },
//     { nev: "pic/characters/6.jpg", cim: "Ricardo Milos", szoveg: "Leírás" },
//     { nev: "pic/characters/7.jpg", cim: "Minecraft Cat", szoveg: "Leírás" },
//     { nev: "pic/characters/8.jpg", cim: "Peter Griffin", szoveg: "Leírás" }
// ];

let kepekAdatok = [];

function beallitMargin(sor) {
    const sorSzelesseg = sor.clientWidth;
    const kepSzelesseg = 100;
    const kepekSzama = sor.children.length;

    let margin = 0;

    if (kepekSzama > 1) {
        margin = (sorSzelesseg - kepSzelesseg * kepekSzama) / (kepekSzama - 1);

        if (!marginNovelve) {
            const pluszMargin = (10 - kepekSzama) * 5 + 10;
            margin += pluszMargin;
            marginNovelve = true;
        }

        if (margin < 0) margin = 0;
    }

    Array.from(sor.children).forEach((img, i) => {
        img.style.marginRight = (i < kepekSzama - 1 ? margin : 0) + "px";
    });
}

function initKepek() {

    const container = document.getElementById("kepekContainer");
    container.innerHTML = "";

    aktualisKivalasztott = null;
    document.querySelectorAll(".kivalasztott")
        .forEach(el => el.classList.remove("kivalasztott"));

    const sor1 = document.createElement("div");
    sor1.classList.add("sor");

    const sor2 = document.createElement("div");
    sor2.classList.add("sor");

    const fele = Math.ceil(kepekAdatok.length / 2);

    kepekAdatok.forEach((adat, index) => {

        const img = document.createElement("img");
        img.src = adat.nev;
        img.dataset.npcId = adat.id;
        img.dataset.picId = adat.picId; 
        img.classList.add("kepek");


        img.addEventListener("click", () => {

            // régi kiválasztás törlése
            if (aktualisKivalasztott) {
                aktualisKivalasztott.classList.remove("kivalasztott");
            }

            img.classList.add("kivalasztott");
            aktualisKivalasztott = img;

            const balDoboz = document.getElementById("balDoboz");
            balDoboz.innerHTML = `<h3>${adat.cim}</h3>${adat.szoveg}`;

            if (!marNovekedett) {

                const doboz = document.getElementById("doboz");
                const cim = document.getElementById("cim");
                const innerCim = document.querySelector(".innerCim");
                const alsoResz = document.getElementById("alsoResz");

                const alsoTeljesMagassag = 120;
                const novekedes = 150;
                const celHeight = doboz.offsetHeight + novekedes;
                const cimTeljesMagassag = innerCim.offsetHeight;

                const interval = 10;
                const step = 2;

                const anim = setInterval(() => {

                    let currentHeight = doboz.offsetHeight;

                    if (currentHeight >= celHeight) {
                        doboz.style.height = celHeight + "px";
                        cim.style.height = cimTeljesMagassag + "px";
                        clearInterval(anim);
                    } else {

                        doboz.style.height = (currentHeight + step) + "px";

                        let newCimHeight =
                            ((currentHeight - (celHeight - novekedes)) / novekedes) * cimTeljesMagassag;

                        if (newCimHeight > cimTeljesMagassag)
                            newCimHeight = cimTeljesMagassag;

                        cim.style.height = newCimHeight + "px";

                        let newAlsoHeight =
                            ((currentHeight - (celHeight - novekedes)) / novekedes) * alsoTeljesMagassag;

                        if (newAlsoHeight > alsoTeljesMagassag)
                            newAlsoHeight = alsoTeljesMagassag;

                        alsoResz.style.height = newAlsoHeight + "px";
                    }

                }, interval);

                marNovekedett = true;
            }
        });

        if (index < fele) sor1.appendChild(img);
        else sor2.appendChild(img);
    });

    container.appendChild(sor1);
    container.appendChild(sor2);

    beallitMargin(sor1);
    beallitMargin(sor2);
}

function initFogadasGomb() {

    const gomb = document.querySelector(".fogadasGomb");

    if (!gomb) {
        console.error("FOGADÁS GOMB NINCS MEGTALÁLVA!");
        return;
    }

    gomb.addEventListener("click", async () => {

        console.log("=== FOGADÁS CLICK ===");

        if (!aktualisKivalasztott) {
            console.warn("NINCS KIVÁLASZTOTT NPC!");
            alert("Először válassz ki egy képet!");
            return;
        }

        const selectElem = document.getElementById("penzOsszeg");

        const rawAmount = selectElem.value;
        if (!rawAmount || rawAmount.trim() === "") {
            alert("Nem írtál be tétet!");
            return;
        }
        const userId = getCookie("userId");

        if (!userId) {
            console.error("NINCS USER ID COOKIE!");
            alert("Nincs bejelentkezett user!");
            return;
        }

        try {
            const gameRes = await fetch('/api/games/current');

            if (!gameRes.ok) {
                console.error("GAME FETCH ERROR:", await gameRes.text());
                throw new Error("Game request failed");
            }

            const game = await gameRes.json();

            console.log("CURRENT GAME:", game);

            const picId = Number(aktualisKivalasztott.dataset.picId);

            console.log("SELECTED ELEMENT:", aktualisKivalasztott);
            console.log("PIC ID FROM UI:", picId);

            const npcsRes = await fetch(`/api/games/${game.id}/npcs`);

            if (!npcsRes.ok) {
                console.error("NPC LIST ERROR:", await npcsRes.text());
                throw new Error("NPC fetch failed");
            }

            const npcs = await npcsRes.json();

            console.log(npcs);

            console.log("PiC LIST:", npcs.map(n => ({
                id: n.id,
                picId: n.picId
            })));

            const amount = Number(selectElem.value.replace(" Ft", ""));

            const request = {
                userId: Number(userId),
                gameId: game.id,
                picId: picId,
                amount: amount
            };

            console.log("FINAL REQUEST:", request);

            // 6. SEND BET
            const response = await fetch('/api/bets', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(request)
            });

            if (!response.ok) {
                if (response.status === 409) {
                    const parsed = await response.json();
                    alert("Nincs elég pénzed!");
                    return; 
                }
            }

            if (response.ok) {

                const result = await response.json();

                console.log("BET SUCCESS RESPONSE:", result);

                alert("Fogadás sikeres!");
                marFogadott = true;
                //localStorage.setItem("fogadott_" + userId, "true");
                await loadCurrentUser();
                updateFogadasGombState();

            } else {

                const text = await response.text();

                console.error("BET ERROR RAW RESPONSE:", text);

                try {
                    const parsed = JSON.parse(text);
                    console.error("BET ERROR PARSED:", parsed);
                } catch (e) {
                    console.error("BET ERROR NOT JSON:", text);
                }

                if (text.includes("Insufficient balance")) {
                    alert("Nincs elég pénzed a fogadáshoz!");
                } else {
                    alert("Hiba történt a fogadás során!");
                }
            }

        } catch (err) {
            console.error("FATAL ERROR IN BETTING:", err);
            alert("Hálózati hiba történt!");
        }

        console.log("=== FOGADÁS CLICK END ===");
    });
}

function updateFogadasGombState() {
    const gomb = document.querySelector(".fogadasGomb");
    const selectElem = document.getElementById("penzOsszeg");
    const penzElem = document.querySelector(".penzSzoveg");

    if (!gomb || !selectElem || !penzElem) return;

    const balance = Number(penzElem.textContent.replace(" Ft", ""));
    const amount = Number(selectElem.value.replace(" Ft", ""));

    const nincsPenz = balance < amount;
    //const alreadyBet = localStorage.getItem("fogadott") === "true";

    if (nincsPenz || marFogadott) {
        gomb.disabled = true;
        gomb.style.opacity = "0.5";
        gomb.style.cursor = "not-allowed";
    } else {
        gomb.disabled = false;
        gomb.style.opacity = "1";
        gomb.style.cursor = "pointer";
    }

}

initFogadasGomb();

const client = new StompJs.Client({
    webSocketFactory: () => new SockJS(window.location.origin + '/ws'),
    onConnect: () => {
        client.subscribe('/topic/game.status', msg => {
            const status = JSON.parse(msg.body);

            if (status.status === 'IN_PROGRESS') {
                window.location.href = '/sim';
            }

            if (status.status === 'FINISHED') {
                console.log("Játék vége → egyenleg frissítés");
                loadCurrentUser(); 
            }
        });
    }
});
client.activate();

async function lekerNpck() {
    const gameRes = await fetch('/api/games/current');
    const game = await gameRes.json();

    const npcsRes = await fetch(`/api/games/${game.id}/npcs`);
    const npcs = await npcsRes.json();

        kepekAdatok = npcs.map(npc => ({
            id: npc.id,
            picId: npc.picId ?? npc.id,   // fallback
            nev: `pic/characters/${npc.picId ?? npc.id}.jpg`,
            cim: npc.name,
            szoveg: `
            <div class="npc-stats">
                <div><span>❤️ Max HP:</span> ${npc.maxHp}</div>
                <div><span>⚡ Sebesség:</span> ${npc.speed}</div>
                <div><span>💥 Sebzés:</span> ${npc.dmg}</div>
                <div><span>♻️ Regeneráció:</span> ${npc.regen}</div>
            </div>
            `
        }));

    initKepek();
    aktualisKivalasztott = null;
    localStorage.setItem("npcCache", JSON.stringify(kepekAdatok));
}

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
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
        console.log("Bejelentkezett felhasználó adatai:", user);
        const penzElem = document.querySelector(".penzSzoveg");
        penzElem.textContent = user.balance + " Ft";
        updateFogadasGombState();
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
            updateFogadasGombState();
        }
    } catch (err) {
        console.error("Hiba a fogadás állapotának lekérdezésekor", err);
    }
}

function initLogout() {
    const btn = document.getElementById("logoutBtn");

    if (!btn) return;

    btn.addEventListener("click", async () => {

        try {
            await fetch("/api/logout", {
                method: "POST",
                credentials: "include"
            });
        } catch (e) {
            console.warn("Logout request failed, continuing...");
        }

        document.cookie = "userId=; Max-Age=0; path=/";

        localStorage.removeItem("savedUsername");

        window.location.href = "/";
    });
}


window.addEventListener('load', () => {
    loadCurrentUser();
    lekerNpck();
    // marFogadott = localStorage.getItem("fogadott") === "true";
    lekerFogadasAllapot();
    // updateFogadasGombState(); 
    initLogout();
});