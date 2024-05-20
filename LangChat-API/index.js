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

app.post("/send-chat", async (req, res) => {
    const message = req.body.message;
    const sender_id = req.body.sender_id;
    const conversation_id = req.body.conversation_id;

    await db.messages.create({
        sender_id,
        message,
        conversation_id,
    });

    amqp.connect("amqp://localhost", function (error0, connection) {
        if (error0) {
            throw error0;
        }
        connection.createChannel(function (error1, channel) {
            if (error1) {
                throw error1;
            }
            var queue = "my_messages";
            var msg = {
                sender_id, message, conversation_id
            };

            channel.assertExchange(queue, "fanout", {
                durable: false,
            });

            channel.sendToQueue(queue, Buffer.from(JSON.stringify(msg)));
            console.log(" [x] Sent %s", msg);
        });
    });
    return res.status(200).json();

});

// Start the Express server
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
