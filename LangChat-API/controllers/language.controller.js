const db = require("../models");
const Language = db.languages;

exports.findAll = async (req, res) => {
    const languages = await Language.findAll();
    let availableLanguages = [];
    for (let lang of languages) {
        availableLanguages.push(lang.name);
    }
    console.log(availableLanguages);
    return res.json(availableLanguages);
};
