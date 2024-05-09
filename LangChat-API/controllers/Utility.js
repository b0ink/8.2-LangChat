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
async function GetUsersConversations(user_id){
    const participantIds = await Participant.findAll({
        where: {
            user_id: userId
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
async function GetConversationMessages(conversation_id){
    const messages = await Message.findAll({
        where: {
            conversation_id: conversation_id
        }
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