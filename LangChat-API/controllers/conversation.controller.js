const db = require("../models");
const User = db.users;
const Translation = db.translations;
const Participant = db.participants;

const { secretKey } = require("../config.json");
const Joi = require("joi");
const jwt = require("jsonwebtoken");

const bcrypt = require("bcrypt");
const saltRounds = 10;

const Utility = require("./Utility");

exports.addParticipant = async (req, res) => {
    const user = req.user;
    const conversationId = parseInt(req.params.conversationId);
    const usernameToAdd = req.body.username;

    // Check if calling user is part of converesation
    const usersConversations = await Utility.GetUsersConversations(user.id);
    if(!usersConversations.includes(conversationId)){
        return res.status(401);
    }
    
    const userToAdd = await User.findOne({
        where: {
            username: usernameToAdd
        }
    });
    
    // Check if user exists
    if(!userToAdd){
        return res.status(404).json("User does not exist")
    }

    // Check if adding user is part of converesation already
    const userToAddConversations = await Utility.GetUsersConversations(userToAdd.id);
    if(userToAddConversations.includes(conversationId)){
        return res.status(401).json("User is already part of the conversation");
    }

    //TODO: check if new user has maxed out conversations they participate in

    const newParticipant = await Participant.create({
        conversation_id: conversationId,
        user_id: userToAdd.id,
        preferredLanguage: userToAdd.defaultPreferredLanguage
    });

    if(newParticipant){
        return res.status(200).json("Added user to conversation");
    }


}


exports.findParticipants = async (req, res) => {
    const user = req.user;
    const conversationId = parseInt(req.params.conversationId);

    // Check if user is part of converesation
    const usersConversations = await Utility.GetUsersConversations(user.id);
    if(!usersConversations.includes(conversationId)){
        return res.status(401);
    }

    const Participants = await Utility.GetConversationParticipants(conversationId);

    let Users = [];
    // Add the calling user to the top of the list
    Users.push({
        username: user.username
    });

    for(let u of Participants){

        if(u.user.username===user.username){
            Users[0]['preferredLanguage'] = u.preferredLanguage;
            continue;
        }

        Users.push({
            username: u.user.username
        });
    }

    console.log(Users)

    return res.json(Users);
}

exports.findMessages = async (req, res) => {
    const user = req.user;
    const conversationId = parseInt(req.params.conversationId);
    const lastMessageId = parseInt(req.params.lastMessageId);

    const usersConversations = await Utility.GetUsersConversations(user.id);
    if(!usersConversations.includes(conversationId)){
        return res.status(401);
    }
    console.log("finding messages in ", conversationId, 'offsetting by', lastMessageId)

    const Messages = await Utility.GetConversationMessages(user, conversationId, lastMessageId);

    // console.log(JSON.stringify(Messages, null, 2));

    return res.json(Messages);
};

exports.saveUsersLanguage = async (req, res) => {
    const user = req.user;
    const conversationId = parseInt(req.body.conversationId);
    const language = req.body.language;

    const usersConversations = await Utility.GetUsersConversations(user.id);
    if(!usersConversations.includes(conversationId)){
        return res.status(401);
    }

    //TODO: validate that `language` exists in the database
    

    const [numberOfAffectedRows, affectedRows] = await Participant.update({
            preferredLanguage: language
        },{
            where: {
                conversation_id: conversationId,
                user_id: user.id
            },
            returning: true,
            plain: true
        }
    )
    
    if(affectedRows > 0){
        return res.json(true)
    }else{
        return res.json(false)
    }
}


exports.sendMessage = async (req, res) => {
    const conversationId = parseInt(req.params.conversationId);
    const message = req.body.message;

    const user = req.user;
    // const user = await db.users.findByPk(req.body.sender_id);

    // TODO: should be unique to the (in this case) participants of active converation
    // TODO: so a custom key defined by userid+conversationId?
    console.log('beggingin send message', conversationId, message, user)

    try {
        const newMessage = await Utility.SendMessage(user.id, conversationId, message);
        
        TranslationService(user, conversationId, newMessage);

        return res.json(newMessage);
    } catch (error) {
        console.log(error)
        return res.status(500);
    }
};

// reponse of sendMessage is used to insert the new bubble client-side, but client should not wait for server-side translations ifrst
async function TranslationService(user, conversationId, newMessage){
    const queue_name = `messages_${conversationId}`;

    const conversationParticipants = await Utility.GetConversationParticipants(conversationId);
    for(let participant of conversationParticipants){
        if(participant.user_id === user.id){
            // Original author will only see their original message
            continue;
        }

        const usersLanguage = participant.preferredLanguage;
        const translatedMessage = await Utility.TranslateMessage(newMessage.message, usersLanguage);
        const translation = await Translation.create({
            message_id: newMessage.id,
            language: usersLanguage,
            message: translatedMessage
        });

        const response = await Utility.NotifyNewMessage(`messages_${conversationId}_${participant.user_id}`);

    }


    // let translatedMesage = {...newMessage};
    // translatedMesage.message = await Utility.TranslateMessage(translatedMesage.message, "spanish");
}


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
