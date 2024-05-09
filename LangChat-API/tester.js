

const db = require("./models/index");

const User = db.users;
const Conversation = db.conversations;
const Message = db.messages;
const Participant = db.participants;
const language = db.participants;

const Utility = require('./controllers/Utility')
async function testMostRecentMessage(){
    const msg = await Utility.GetMostRecentConversationMessage(1);

    const messageData = msg.toJSON();
    
    // Print the formatted message
    console.log(JSON.stringify(messageData, null, 2));
    
    // console.log(msg);
}

// testMostRecentMessage();



async function getAllConversationMessages(){

    const conversationIds = await Utility.GetUsersConversations(1);
    console.log(conversationIds)

    const messages = await Utility.GetConversationMessages(conversationIds[0]);

    for(let msg of messages){
        const messageData = msg.toJSON();
        console.log(JSON.stringify(messageData, null, 2));

    }
    
}

// getAllConversationMessages();



async function sendMessageToConversation(sender_id, conversation_id, message){

    const newMessage = await Utility.SendMessage(sender_id, conversation_id, message)
    console.log(newMessage)
}

// sendMessageToConversation(1, 1, "Hello world...");


async function getConversations(){

    const userId = 1;
    const conversationIds = await Utility.GetUsersConversations(userId);

    //TODOL GetUsersConversations should return this data natively
    let conversations = [];

    console.log('cponvo ids', conversationIds);

    for(let conversation_id of conversationIds){
        console.log(conversation_id)
        const participants = await Utility.GetConversationParticipants(conversation_id);

        const msg = await Utility.GetMostRecentConversationMessage(1);


        conversations.push({
            // participants: [...participants.filter(p=>p.user.username!=="bob")],
            participants: [...participants],
            lastMessage: msg
        })

        
    }

    console.log(JSON.stringify(conversations, null, 2))

}

getConversations();