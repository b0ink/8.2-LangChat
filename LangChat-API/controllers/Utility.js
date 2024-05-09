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
            user_id: user_id
        },
        attributes: ['conversation_id']
    });

    const conversationIds = participantIds.map(participant => participant.conversation_id);

    return conversationIds;
}


/**
 * 
 * @param INTEGER conversation_id 
 * @returns An array of Message objects
 */
module.exports.GetConversationMessages = async (conversation_id) => {
    const messages = await Message.findAll({
        where: {
            conversation_id: conversation_id
        },
        include: [{
            model: User,
            as: 'user', 
            attributes: ['username']
        }]
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
}

/**
 * 
 * @param INTEGER conversation_id 
 * @returns Message object
 */
module.exports.GetMostRecentConversationMessage = async (conversation_id) => {
    const message = await Message.findOne({
        where: {
            conversation_id: conversation_id
        },
        order: [['createdAt', 'DESC']],
        include: [{
            model: User,
            as: 'user', 
            attributes: ['username']
        }]
    });

    return message;
}


module.exports.SendMessage = async (sender_id, conversation_id, message) => {
    const user = await User.findByPk(sender_id);

    if(!user){
        console.log(`Sender id:${sender_id} does not exist`);
        return;
    }

    const conversationIds = await this.GetUsersConversations(user.id);
    if(!conversationIds.includes(conversation_id)){
        console.log(conversationIds, conversation_id);
        console.log(`SendMessage: Sender is not a part of this conversation`)
        return ;
    }

    const newMesssage = await Message.create({
        conversation_id,
        sender_id,
        message
    });
    
    console.log(newMesssage.toJSON());
    return newMesssage;
}