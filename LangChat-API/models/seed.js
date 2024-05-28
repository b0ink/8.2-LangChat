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

// SeedData_1();

async function SeedData_2(){
    const bob = 1;
    const bill = 2;
    const james = 3;

    const conv1 = await CreateConversation();
    await AssignParticipants(conv1.id, bob);
    await AssignParticipants(conv1.id, bill);

    const conv2 = await CreateConversation();
    await AssignParticipants(conv2.id, bob);
    await AssignParticipants(conv2.id, james);

    const conv3 = await CreateConversation();
    await AssignParticipants(conv3.id, bob);
    await AssignParticipants(conv3.id, james);
    await AssignParticipants(conv3.id, bill);

    const messages1 = [
        { userId: bob, text: "Hey Bill" },
        { userId: bill, text: "Hey Bob" },
        { userId: bob, text: "How's it going" },
        { userId: bill, text: "Pretty good you" },
        { userId: bob, text: "Not bad" },
        { userId: bob, text: "Did you finish the report" },
        { userId: bill, text: "Almost done" },
        { userId: bob, text: "Cool" },
        { userId: bill, text: "When is it due" },
        { userId: bob, text: "End of the week" },
        { userId: bill, text: "Got it" }
    ];

    const messages2 = [
        { userId: bob, text: "Hey James" },
        { userId: james, text: "Hey Bob" },
        { userId: bob, text: "What are you up to" },
        { userId: james, text: "Just working" },
        { userId: bob, text: "Same here" },
        { userId: bob, text: "Do you want to grab lunch later" },
        { userId: james, text: "Sure what time" },
        { userId: bob, text: "Around 1" },
        { userId: james, text: "Sounds good" },
        { userId: bob, text: "See you then" }
    ];

    const messages3 = [
        { userId: bob, text: "Hey guys" },
        { userId: bill, text: "Hey Bob" },
        { userId: james, text: "Hey Bob" },
        { userId: bob, text: "What are you both doing" },
        { userId: bill, text: "Just working" },
        { userId: james, text: "Same here" },
        { userId: bob, text: "Anyone up for a coffee later" },
        { userId: bill, text: "I can do that" },
        { userId: james, text: "Me too" },
        { userId: bob, text: "Great let's meet at 3" },
        { userId: bill, text: "Sounds good" },
        { userId: james, text: "See you then" }
    ];

    for (const msg of messages1) {
        await CreateMessage(conv1.id, msg.userId, msg.text);
    }

    for (const msg of messages2) {
        await CreateMessage(conv2.id, msg.userId, msg.text);
    }

    for (const msg of messages3) {
        await CreateMessage(conv3.id, msg.userId, msg.text);
    }
}

SeedData_2();
