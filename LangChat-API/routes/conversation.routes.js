
const {authenticateToken} = require('../Utility.js')

module.exports = (app, API_VERSION) => {
    const conversation = require("../controllers/conversation.controller.js");
    var router = require("express").Router();


    //TODO: under authentication
    router.post("/messages", conversation.findMessages);

    router.post("/send-message", conversation.sendMessage);

    app.use(`/api/${API_VERSION}/conversation`, router);
};