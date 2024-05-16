const db = require("../models");
const User = db.users;

const { secretKey } = require("../config.json");
const Joi = require("joi");
const jwt = require("jsonwebtoken");

const bcrypt = require("bcrypt");
const saltRounds = 10;

const Utility = require("./Utility");

exports.findMessages = async (req, res) => {
    const user = req.user;
    const conversationId = req.params.conversationId;

    const usersConversations = await Utility.GetUsersConversations(user.id);
    if(!usersConversations.includes(conversationId)){
        return res.status(401);
    }

    const Messages = await Utility.GetConversationMessages(conversationId);

    console.log(JSON.stringify(Messages, null, 2));

    return res.json(Messages);
};

exports.sendMessage = async (req, res) => {
    const conversationId = parseInt(req.params.conversationId);
    const message = req.body.message;

    const user = req.user;
    // const user = await db.users.findByPk(req.body.sender_id);

    // TODO: should be unique to the (in this case) participants of active converation
    // TODO: so a custom key defined by userid+conversationId?
    const queue_name = `messages_${conversation_id}`;

    try {
        const newMessage = await Utility.SendMessage(user.id, conversationId, message);
        // let translatedMesage = {...newMessage};
        // translatedMesage.message = await Utility.TranslateMessage(translatedMesage.message, "spanish");
        // const response =  await Utility.NotifyNewMessage(queue_name, translatedMesage);

        return res.json(newMessage);
    } catch (error) {
        console.log(error)
        return res.status(500);
    }
};


exports.translateMessage = async (req, res) => {
    const user = req.user;

    //TODO body validation
    const messageId = req.body.messageId;
    const usersLanguage = req.body.usersLanguage;

    const message = await db.messages.findByPk(messageId);
    if(message == null){
        return res.status(404);
    }

    // const user = await db.users.findByPk(req.body.sender_id);
    // if(user == null){
    //     return res.status(401);
    // }

    //TODO: provide context to llama by retrieving the last 10 messages in the conversation and embed in prompt
    const conversations = await Utility.GetUsersConversations(user.id);
    if(!conversations.includes(message.conversation_id)){
        return res.status(401);
    }

    //TODO: validate usersLangage
    const translation = await Utility.TranslateMessage(message.message, usersLanguage);


      const translatedMessage = {
        id: message.id,
        conversation_id: message.conversation_id,
        sender_id: message.sender_id,
        message: translation,
        createdAt: message.createdAt,
        updatedAt: message.updatedAt,
        user: {
            username: user.username
        }
    }
    translatedMessage.message = translation;
    console.log('translatedMessage')
    console.log(translatedMessage)

    return res.status(200).json(translatedMessage);

    console.log(typeof(conversation_id));

};
