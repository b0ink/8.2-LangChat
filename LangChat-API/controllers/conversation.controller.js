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
        username: "bob"
    };

    const conversation_id = req.body.conversation_id;


    const Messages = await Utility.GetConversationMessages(conversation_id);
    
    console.log('111')
    console.log(JSON.stringify(Messages, null, 2));

    return res.json(Messages);
};
