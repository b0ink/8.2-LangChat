module.exports = (app, API_VERSION) => {
    const users = require("../controllers/user.controller.js");

    var router = require("express").Router();

    // Create a new User
    router.post("/register", users.create);

    router.post("/login", users.findOne);

    app.use(`/api/${API_VERSION}/users`, router);
};