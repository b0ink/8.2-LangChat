const express = require("express");
const jwt = require("jsonwebtoken");

const app = express();
const PORT = process.env.PORT || 3000;
var bodyParser = require("body-parser");

const API_VERSION = "v0";

const { GradientLLM } = require("@langchain/community/llms/gradient_ai");

const { accessToken, workspaceId } = require("./config.json");

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));

require("./routes/user.routes")(app, API_VERSION);
require("./routes/conversation.routes")(app, API_VERSION);
require("./routes/language.routes")(app, API_VERSION);

// testLanguage("What's the whether like today?", "Portugese");
// testLanguage("omg lol", "portugese");
// testLanguage("whats the whether like today?", "korean");
// testLanguage("My name is john", "spanish");

const amqp = require("amqplib/callback_api");

const db = require("./models/index");

const Languages = ["English", "Spanish", "French", "German", "Italian", "Portugese", "Dutch", "Russian", "Chinese", "Japanese", "Korean"]

async function SeedLanguages(){
    let langs = [];
    
    for(let l of await db.languages.findAll({plain: false, raw: true})){
        langs.push(l.name)
    }

    for(let l of Languages){
        if(!langs.includes(l)){
            await db.languages.create({name: l})
        }
    }

}

SeedLanguages();

// Start the Express server
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
