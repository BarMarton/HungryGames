let login = document.getElementById("login")
let register = document.getElementById("reg")

let login_see = true


function switchLogReg(){
    if(login_see){
        register.style.display = "flex"
        login.style.display = "none"
        login_see = false
    } else {
        register.style.display = "none"
        login.style.display = "flex"
        login_see = true
    }
}

function regPressed(){
    let inputs = document.querySelectorAll("#reg input");
    
    if (inputs[2].value !== inputs[3].value){
        alert("The passwords aren't matching!");
        return;
    } 
    
    const userData = {
        username: inputs[0].value,
        email: inputs[1].value,
        password: inputs[2].value
    };
    sendToServer(userData);
}

function sendToServer(data) {
    const params = new URLSearchParams(data); 

    fetch('/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(response => {
        if(response.ok) {
            alert("Sikeres regisztráció!");
            switchLogReg(); 
        } else {
            alert("Hiba történt, vagy a név már foglalt!");
        }
    })
    .catch(error => console.error("Hiba történt:", error));
}

function logPressed() {
    let inputs = document.querySelectorAll("#login input");
    let isRemembered = document.getElementById("remember_me").checked;
    if (isRemembered) {
        localStorage.setItem("savedUsername", inputs[0].value);
        alert("Saved")
    } else {
        localStorage.removeItem("savedUsername");
    }


    const loginData = {
        username: inputs[0].value,
        password: inputs[1].value
    };
    
    sendLoginToServer(loginData);
}

function sendLoginToServer(data) {
    const params = new URLSearchParams(data);

    fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(response => {
        if(response.redirected) {
            window.location.href = response.url;
        } else if(response.ok) {
            alert("Sikeres bejelentkezés!");
        } else {
            alert("Hibás felhasználónév vagy jelszó!");
        }
    })
    .catch(error => console.error("Hiba történt:", error));
}

window.onload = () => {
    let savedName = localStorage.getItem("savedUsername");
    if (savedName) {
        document.querySelectorAll("#login input")[0].value = savedName;
        document.getElementById("remember_me").checked = true;
    }
};