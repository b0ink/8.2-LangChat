const { Sequelize } = require("sequelize");
const config = require("../config.json");
const { x } = require("joi");

const sequelize = new Sequelize(config.DB_DATABASE, config.DB_USER, config.DB_PASS, {
    host: config.DB_HOST,
    port: config.SQL_PORT,
    dialect: "mysql",
    sync: false,
    dialectOptions: {
        // hotfix for my xampp installation of mysql/phpmyadmin
        socketPath: "/Applications/XAMPP/xamppfiles/var/mysql/mysql.sock",
    },
});

const db = {};

db.Sequelize = Sequelize;
db.sequelize = sequelize;

db.users = require("./user.model.js")(sequelize, Sequelize);
db.conversations = require("./conversation.model.js")(sequelize, Sequelize);
db.languages = require("./language.model.js")(sequelize, Sequelize);
db.messages = require("./message.model.js")(sequelize, Sequelize);
db.participants = require("./participant.model.js")(sequelize, Sequelize);

async function initData() {
    const user = await db.users.create({
        username: "bob",
        email: "asdf@asdf.com",
        password: "123",
    });

    const newConvo = await db.conversations.create();

    await db.participants.create({
        conversation_id: newConvo.id,
        user_id: user.id,
    });

    const msg = await db.messages.create({
        conversation_id: newConvo.id,
        sender_id: user.id,
        message: "Hello world!",
    });
}
// initData();


async function getData(){
    const userId = 1; // bob

    const participantIds = await db.participants.findAll({
        where: {
            user_id: userId
        },
        attributes: ['conversation_id']
    });

    const conversationIds = participantIds.map(participant => participant.conversation_id);
    console.log(conversationIds)

    const messages = await getConversationMessages(conversationIds[0])

    messages.forEach(message => {
        console.log('---------------------');
        console.log(`Message ID: ${message.id}`);
        console.log(`Sender ID: ${message.sender_id}`);
        console.log(`Message: ${message.message}`);
        console.log(`Sent At: ${message.sentAt}`);
        console.log('---------------------');
    });

    // return conversations;
}

async function getConversationMessages(conversationId) {
    try {
        const messages = await db.messages.findAll({
            where: {
                conversation_id: conversationId
            }
        });

        return messages;
    } catch (error) {
        console.error('Error fetching conversation messages:', error);
        throw error;
    }
}

getData();

// sequelize.sync({force: true});

module.exports = db;
