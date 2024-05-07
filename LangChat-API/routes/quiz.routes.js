
const {authenticateToken} = require('../Utility.js')

module.exports = (app, API_VERSION) => {
    const quiz = require("../controllers/quiz.controller.js");
    var router = require("express").Router();

    // router.get("/create", authenticateToken, quiz.create);

    // router.get("/", authenticateToken, quiz.findAll);

    // router.post("/feedback", authenticateToken, quiz.getFeedback);

    app.use(`/api/${API_VERSION}/quiz`, router);
};