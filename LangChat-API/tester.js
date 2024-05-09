

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
    const messages = await Utility.GetConversationMessages(1);

    for(let msg of messages){
        const messageData = msg.toJSON();
        console.log(JSON.stringify(messageData, null, 2));

    }
    
}

getAllConversationMessages();
