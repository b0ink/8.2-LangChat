const { authenticateToken } = require("../Authentication.js");

module.exports = (app, API_VERSION) => {
    const users = require("../controllers/user.controller.js");

    var router = require("express").Router();

    // Create a new User
    router.post("/register", users.create);

    router.post("/login", users.findOne);

    router.get("/conversations", authenticateToken, users.findConversations);

    router.get("/get-language", authenticateToken, users.getLanguage);
    router.post("/save-language", authenticateToken, users.saveLanguage);

    app.use(`/api/${API_VERSION}/users`, router);
};
