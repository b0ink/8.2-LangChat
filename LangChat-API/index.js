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

async function testLanguage(text, language){
    // const query = `[INST]
    // Translate the following text into this language: ${language}.
    // ${text}

    // Please then correct the translation to ensure it accurately reflects the original text message.
    // If the original text appears to be casual, the translation should be casual.
    // If the original text appears to be rather formal, the translation should remain formal.
    // Do not respond in phonetic language. remove any quote marks unless the original text contained it.
    // Respond in the following format:
    // TRANSLATION: {final translation}
    // NOTE: {any notes on the translation}

    // TRANSLATION should not contain any parentheses or any quote pmarks
    // NOTE should all be a single line and concise.
    // NOTE should remain in the same language as the original text
    // Do not refer to yourself in the note.
    // Keep the note short. If you believe there is no need for note, leave it empty.
    // Avoid mentioning if the original text and translation is casual/formal in the note.
    // Use note for when typos or mistakes were detected and corrected. You can also mention what the translation literally means back in the original language, in the note.
    // [/INST]`;

    const query = `[INST]
    Translate the following text into this language: ${language}.
    ${text}

    Please then correct the translation to ensure it accurately reflects the original text message in terms of tone and purpose.
    If the original text appears to be casual, the translation should be casual.
    If the original text appears to be rather formal, the translation should remain formal.
    Do not respond in phonetic language. remove any quote marks unless the original text contained it.
    Respond in the following format:
    TRANSLATION: {final translation}

    TRANSLATION should not contain any parentheses or any quote marks.
    If you are unable to translate the message, leave TRANSLATION blank.
    [/INST]`;

    const model = new GradientLLM({
        gradientAccessKey: accessToken,
        workspaceId,
        // modelSlug: "mixtral-8x7b-instruct",
        modelSlug: "llama3-70b-chat",
        // modelSlug: "bloom-560m",
        inferenceParameters: {
            maxGeneratedTokenCount: 500,
            temperature: 0.01,
        },
    });

    const result = await model.invoke(query);
    // console.log(text);
    // console.log()
    // console.log(result)
    // console.log(JSON.stringify(result));
    const {translation, note} = parseTranslationReponse(result);
    console.log(translation)
    console.log(note)
}


function parseTranslationReponse(text){
    const lines = text.split("\n");
    let translation = null;
    let note = null

    for(let line of lines){
        const text = line.trim();

        if(text.startsWith("TRANSLATION: ")){
            translation = text.substr("TRANSLATION: ".length).trim();
            continue;
        }
        if(text.startsWith("NOTE: ")){
            note = text.substr("NOTE: ".length).trim();
            console.log('found note')
        }

        
    }
    return {
        translation,
        note
    }
}
// testLanguage("What's the whether like today?", "Portugese");
// testLanguage("omg lol", "portugese");
// testLanguage("whats the whether like today?", "korean");
// testLanguage("My name is john", "spanish");

// Start the Express server
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
