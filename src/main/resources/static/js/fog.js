let marNovekedett = false;
let marginNovelve = false;
let aktualisKivalasztott = null;

let kepekAdatok = [
    { nev: "pic/characters/1.jpg", cim: "Gigachad", szoveg: "Leírás" },
    { nev: "pic/characters/2.jpg", cim: "Shrek", szoveg: "Leírás." },
    { nev: "pic/characters/3.jpg", cim: "Arató András", szoveg: "Leírás" },
    { nev: "pic/characters/4.jpg", cim: "Homer Simpson", szoveg: "Leírás" },
    { nev: "pic/characters/5.jpg", cim: "Mike Wazowski", szoveg: "Leírás" },
    { nev: "pic/characters/6.jpg", cim: "Ricardo Milos", szoveg: "Leírás" },
    { nev: "pic/characters/7.jpg", cim: "Minecraft Cat", szoveg: "Leírás" },
    { nev: "pic/characters/8.jpg", cim: "Peter Griffin", szoveg: "Leírás" }
];

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

    const sor1 = document.createElement("div");
    sor1.classList.add("sor");

    const sor2 = document.createElement("div");
    sor2.classList.add("sor");

    const fele = Math.ceil(kepekAdatok.length / 2);

    kepekAdatok.forEach((adat, index) => {

        const img = document.createElement("img");
        img.src = adat.nev;
        img.classList.add("kepek");

        img.addEventListener("click", () => {

          
            if (aktualisKivalasztott !== null) {
                aktualisKivalasztott.classList.remove("kivalasztott");
            }
            img.classList.add("kivalasztott");
            aktualisKivalasztott = img;

            
            const balDoboz = document.getElementById("balDoboz");
            balDoboz.innerHTML = `<h3>${adat.cim}</h3><p>${adat.szoveg}</p>`;

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

    if (!gomb) return;

    gomb.addEventListener("click", () => {

        if (!aktualisKivalasztott) {
            alert("Először válassz ki egy képet!");
            return;
        }

        const selectElem = document.getElementById("penzOsszeg");
        const kepNev = aktualisKivalasztott.src.split("/").pop();

        const adat = {
            kivalasztottKep: kepNev.replace(".jpg", ""),
            penz: selectElem.value.replace("Ft", "")
        };

        console.log(JSON.stringify(adat));
    });
}

initKepek();
initFogadasGomb();