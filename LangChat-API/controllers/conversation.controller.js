const db = require("../models");
const User = db.users;

const { secretKey } = require("../config.json");
const Joi = require("joi");
const jwt = require("jsonwebtoken");

const bcrypt = require("bcrypt");
const saltRounds = 10;

const Utility = require("./Utility");

exports.findMessages = async (req, res) => {
    // const user = req.user;
    const user = {
        id: 1,
        username: "bob",
    };

    const conversation_id = req.body.conversation_id;

    const Messages = await Utility.GetConversationMessages(conversation_id);

    console.log("111");
    console.log(JSON.stringify(Messages, null, 2));

    return res.json(Messages);
};

exports.sendMessage = async (req, res) => {
    //TODO body validation
    const conversation_id = parseInt(req.body.conversation_id);
    const message = req.body.message;
    const user = {
        id: 1,
        username: "bob",
    };

    // TODO: should be unique to the (in this case) participants of active converation
    // TODO: so a custom key defined by userid+conversationId?
    const queue_name = `messages_${conversation_id}`;

    console.log(typeof(conversation_id));

    try {
        const newMessage = await Utility.SendMessage(user.id, conversation_id, message);
        const response =  await Utility.NotifyNewMessage(queue_name, newMessage);
        console.log(response);
        return res.json(newMessage);
    } catch (error) {
        console.log(error)
        return res.status(500);
    }
};
