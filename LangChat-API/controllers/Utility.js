// const amqp = require("amqplib/callback_api");
const amqp = require("amqplib");

const db = require("../models/index");

const User = db.users;
const Conversation = db.conversations;
const Message = db.messages;
const Participant = db.participants;
const language = db.languages;
const Translation = db.translations;

const { GradientLLM } = require("@langchain/community/llms/gradient_ai");

const { accessToken, workspaceId } = require("../config.json");

/**
 * Retrieve all conversations a user is a participant in
 *
 * @param INTEGER user's ID
 * @returns An array of conversation IDs
 */
module.exports.GetUsersConversations = async (user_id) => {
    const participantIds = await Participant.findAll({
        where: {
            user_id: user_id,
        },
        attributes: ["conversation_id"],
    });

    const conversationIds = participantIds.map((participant) => participant.conversation_id);

    return conversationIds;
};

module.exports.GetConversationParticipants = async (conversation_id) => {
    const participants = await Participant.findAll({
        where: {
            conversation_id: conversation_id,
        },
        include: [
            {
                model: User,
                as: "user",
                attributes: ["id", "username", "defaultPreferredLanguage"],
            },
        ],
    });
    
    console.log("participants", participants, conversation_id);
    return participants;
};

/**
 *
 * @param INTEGER conversation_id
 * @returns An array of Message objects
 */
module.exports.GetConversationMessages = async (user, conversation_id, lastMessageId = -1) => {
    const participant = await Participant.findOne({
        where:{
            conversation_id: conversation_id,
            user_id: user.id
        }
    });

    lastMessageId = lastMessageId - 5; // ??

    let preferredLanguage = participant.preferredLanguage;
    console.log("preferred lang", preferredLanguage);
    const messages = await Message.findAll({
        where: {
            conversation_id: conversation_id,
            id: {
                [db.Sequelize.Op.gte]: lastMessageId // id greater than to lastMessageId
            }
        },
        limit: 150,
        include: [
            {
                model: User,
                as: "user",
                attributes: ["username"],
            },
            {
                model: Translation,
                as: 'translations',
                required: false,
                where: { language: preferredLanguage },
                attributes: ['message_id', 'language', 'message']
            }
        ],
    });

    console.log(messages)

    return messages;
};

/**
 *
 * @param INTEGER conversation_id
 * @returns Message object
 */
module.exports.GetMostRecentConversationMessage = async (user_id, conversation_id) => {

    const user = await User.findByPk(user_id);
    if(!user){
        return [];
    }

    let preferredLanguage = user.defaultPreferredLanguage;

    const userAsParticipant = await Participant.findOne({
        where: {
            user_id: user.id,
            conversation_id: conversation_id
        }
    });

    if(userAsParticipant){
        preferredLanguage = userAsParticipant.preferredLanguage;
    }
    

    const message = await Message.findOne({
        where: {
            conversation_id: conversation_id,
        },
        order: [["createdAt", "DESC"]],
        include: [
            {
                model: User,
                as: "user",
                attributes: ["username"],
            },
            {
                model: Translation,
                as: 'translations',
                required: false,
                where: { language: preferredLanguage },
                attributes: ['message_id', 'language', 'message']
            }
        ]
    });

    console.log(message)

    return message;
};


module.exports.SendSystemMessage = async (conversation_id, message) => {
    const systemMessage = await Message.create({
        conversation_id,
        sender_id: 0,
        message,
    });

    const conversation = await Conversation.findByPk(conversation_id);
    if(conversation && systemMessage){
        conversation.changed('updatedAt', true);
        await conversation.save();
    }

    const conversationParticipants = await this.GetConversationParticipants(conversation_id);
    setTimeout(()=>{
        for(let p of conversationParticipants){
                this.NotifyNewMessage(`messages_${conversation_id}_${p.user_id}`);
        }
    }, 1000);
}

module.exports.SendMessage = async (sender_id, conversation_id, message) => {
    const user = await User.findByPk(sender_id);

    if (!user) {
        console.log(`Sender id:${sender_id} does not exist`);
        throw new Error("Sender id is invalid");
    }

    const conversationIds = await this.GetUsersConversations(user.id);
    if (!conversationIds.includes(conversation_id)) {
        console.log(conversationIds, conversation_id);
        console.log(`SendMessage: Sender is not a part of this conversation`);
        throw new Error("Sender is not part of this converation");
    }

    const newMesssage = await Message.create({
        conversation_id,
        sender_id,
        message,
    });

    const messageData = {
        id: newMesssage.id,
        conversation_id,
        sender_id,
        message,
        createdAt: newMesssage.createdAt,
        updatedAt: newMesssage.updatedAt,
        user: {
            username: user.username
        }
    }
    console.log(messageData)
    // console.log(messageData.toJSON());
    return messageData;
};

module.exports.NotifyNewMessage = (queue, payload) => {
    return new Promise((resolve, reject) => {
        amqp.connect("amqp://localhost")
            .then((conn) => {
                return conn.createChannel();
            })
            .then((channel) => {
                channel.assertExchange(queue, "fanout", {
                    durable: false,
                });
                // channel.sendToQueue(queue, Buffer.from(JSON.stringify(payload)));
                channel.sendToQueue(queue, Buffer.from("Success"));

                console.log(" [x] Sent succes to ", queue);
                resolve(`Send ${payload} to ${queue}`);
            })
            .catch((error) => {
                console.log('erropr', error);
                reject(new Error(error));
            });
    });
};



module.exports.TranslateMessage = async (text, language, sourceLanguage) => {
    // const query = `[INST]
    // Translate the following text into this language: ${language}.
    // ${text}

    // Please then correct the translation to ensure it accurately reflects the original text message.
    // If the original text appears to be casual, the translation should be casual.
    // If the original text appears to be rather formal, the translation should remain formal.
    // Do not respond in phonetic language. remove any quote marks unless the original text contained it.
    // Respond in the following format:
    // TRANSLATION: {final translation}
    // NOTE: {any notes on the translation}

    // TRANSLATION should not contain any parentheses or any quote pmarks
    // NOTE should all be a single line and concise.
    // NOTE should remain in the same language as the original text
    // Do not refer to yourself in the note.
    // Keep the note short. If you believe there is no need for note, leave it empty.
    // Avoid mentioning if the original text and translation is casual/formal in the note.
    // Use note for when typos or mistakes were detected and corrected. You can also mention what the translation literally means back in the original language, in the note.
    // [/INST]`;

    const query = `[INST]
    Translate the following text into this language: ${language}. The source language is possibly ${sourceLanguage}.
    If you do not understand the slang or abbreviations, interpret it as if its from the source language.
    The source language may be different to what the actual text is, translate it to the required language regardless.
    If you are unable to translate the message, return nothing. Do not reply saying you do not understand or were unable to translate.
    Here is the text: ${text}

    Please then correct the translation to ensure it accurately reflects the original text message in terms of tone and purpose.
    Try to repeat the same capitalisation. If the the original text and the text you were translating to look the same, or is the same language, return nothing.
    If the original text appears to be casual, the translation should be casual.
    If the original text appears to be rather formal, the translation should remain formal.
    Do not respond in phonetic language. Remove any quote marks unless the original text contained it.
    Respond in the following format:
    TRANSLATION: {final translation}

    TRANSLATION should not contain any parentheses or any quote marks.
    If you are unable to translate the message, leave TRANSLATION blank.
    [/INST]`;

    const model = new GradientLLM({
        gradientAccessKey: accessToken,
        workspaceId,
        // modelSlug: "mixtral-8x7b-instruct",
        modelSlug: "llama3-70b-chat",
        // modelSlug: "bloom-560m",
        inferenceParameters: {
            maxGeneratedTokenCount: 500,
            temperature: 0.01,
        },
    });

    const result = await model.invoke(query);
    // console.log(text);
    // console.log()
    // console.log(result)
    // console.log(JSON.stringify(result));
    const { translation, note } = parseTranslationReponse(result);
    console.log(translation);
    console.log(note);
    return translation;
}

function parseTranslationReponse(text) {
    const lines = text.split("\n");
    let translation = null;
    let note = null;

    for (let line of lines) {
        const text = line.trim();

        if (text.startsWith("TRANSLATION: ")) {
            translation = text.substr("TRANSLATION: ".length).trim();
            continue;
        }
        if (text.startsWith("NOTE: ")) {
            note = text.substr("NOTE: ".length).trim();
            console.log("found note");
        }
    }
    return {
        translation,
        note,
    };
}


// if isGroupChat==true, user1 will be made admin of the group chat
module.exports.CreateConversation = async (user1, user2, isGroupChat=false) => {
    const newConversation = await Conversation.create();
    const user1Participant = await Participant.create({
        conversation_id: newConversation.id,
        user_id: user1.id,
        preferredLanguage: user1.defaultPreferredLanguage,
        isAdmin: isGroupChat?true:false
    });

    const user2Participant = await Participant.create({
        conversation_id: newConversation.id,
        user_id: user2.id,
        preferredLanguage: user2.defaultPreferredLanguage
    });
    return newConversation;
}