// const amqp = require("amqplib/callback_api");
const amqp = require("amqplib");

const db = require("../models/index");

const User = db.users;
const Conversation = db.conversations;
const Message = db.messages;
const Participant = db.participants;
const language = db.participants;

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
                attributes: ["username"],
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
module.exports.GetConversationMessages = async (conversation_id) => {
    const messages = await Message.findAll({
        where: {
            conversation_id: conversation_id,
        },
        include: [
            {
                model: User,
                as: "user",
                attributes: ["username"],
            },
        ],
    });

    // messages.forEach(message => {
    //     console.log('---------------------');
    //     console.log(`Message ID: ${message.id}`);
    //     console.log(`Sender ID: ${message.sender_id}`);
    //     console.log(`Message: ${message.message}`);
    //     console.log(`Sent At: ${message.sentAt}`);
    //     console.log('---------------------');
    // });

    return messages;
};

/**
 *
 * @param INTEGER conversation_id
 * @returns Message object
 */
module.exports.GetMostRecentConversationMessage = async (conversation_id) => {
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
        ],
    });

    return message;
};

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
                channel.sendToQueue(queue, Buffer.from(JSON.stringify(payload)));

                console.log(" [x] Sent %s", payload);
                resolve(`Send ${payload} to ${queue}`);
            })
            .catch((error) => {
                console.log('erropr', error);
                reject(new Error(error));
            });
    });
};
