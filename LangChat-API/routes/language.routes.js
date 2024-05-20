const { authenticateToken } = require("../Authentication.js");

module.exports = (app, API_VERSION) => {
    const languages = require("../controllers/language.controller.js");

    var router = require("express").Router();

    router.get("/", authenticateToken, languages.findAll);

    app.use(`/api/${API_VERSION}/languages`, router);
};
