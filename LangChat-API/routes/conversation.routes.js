
const {authenticateToken} = require('../Authentication.js')

module.exports = (app, API_VERSION) => {
    const conversation = require("../controllers/conversation.controller.js");
    var router = require("express").Router();

    
    router.get("/:conversationId/participants", authenticateToken, conversation.findParticipants);

    router.get("/:conversationId/messages/:lastMessageId", authenticateToken, conversation.findMessages);

    router.post("/:conversationId/send-message", authenticateToken, conversation.sendMessage);

    router.post("/save-language", authenticateToken, conversation.saveUsersLanguage);

    router.post("/translate", authenticateToken, conversation.translateMessage);

    app.use(`/api/${API_VERSION}/conversation`, router);
};