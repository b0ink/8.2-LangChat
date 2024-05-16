
const {authenticateToken} = require('../Authentication.js')

module.exports = (app, API_VERSION) => {
    const conversation = require("../controllers/conversation.controller.js");
    var router = require("express").Router();

    router.get("/:conversationId/messages", authenticateToken, conversation.findMessages);

    router.post("/:conversationId/send-message", authenticateToken, conversation.sendMessage);

    router.post("/translate", authenticateToken, conversation.translateMessage);

    app.use(`/api/${API_VERSION}/conversation`, router);
};