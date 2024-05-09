

const db = require("./models/index");

const User = db.users;
const Conversation = db.conversations;
const Message = db.messages;
const Participant = db.participants;
const language = db.participants;

const Utility = require('./controllers/Utility')
async function test(){
    const msg = await Utility.GetMostRecentConversationMessage(1);

    const messageData = msg.toJSON();
    
    // Print the formatted message
    console.log(JSON.stringify(messageData, null, 2));
    
    // console.log(msg);
}

test();
