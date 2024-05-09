/* Debug purposes only */

const db = require("./index");

const User = db.users;
const Conversation = db.conversations;
const Message = db.messages;
const Participant = db.participants;
const language = db.participants;

async function SyncTables() {
    await db.sequelize.sync({ force: true });
}

async function CreateUser(username) {
    const user = await db.users.create({
        username: username,
        email: "test@test.com",
        password: "abc123",
    });

    return user;
}

async function CreateConversation() {
    const newConvo = await db.conversations.create();
    return newConvo;
}

async function AssignParticipants(convo_id, user_id) {
    await db.participants.create({
        conversation_id: convo_id,
        user_id: user_id,
    });
}

async function CreateMessage(conversation_id, sender_id, message) {
    const msg = await db.messages.create({
        conversation_id,
        sender_id,
        message,
    });
}

async function SeedData_1() {
    await SyncTables();

    // create users
    const user1 = await CreateUser("bob");
    const user2 = await CreateUser("bill");

    // init conversation
    const convo = await CreateConversation();

    // assign user1 & user2 to conversation
    await AssignParticipants(convo.id, user1.id);
    await AssignParticipants(convo.id, user2.id);

    await CreateMessage(convo.id, user1.id, "Hello 1");
    await CreateMessage(convo.id, user2.id, "Hi");
    await CreateMessage(convo.id, user2.id, "whats up");
    await CreateMessage(convo.id, user1.id, "not much");
}

SeedData_1();
