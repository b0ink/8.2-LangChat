const { DataTypes } = require("sequelize");

module.exports = (sequelize, Sequelize) => {
    const Message = sequelize.define("message", {
        id: {
            type: DataTypes.INTEGER,
            primaryKey: true,
            autoIncrement: true,
        },
        conversation_id: {
            type: DataTypes.INTEGER,
            references: {
                model: "conversations",
                key: "id",
            },
        },
        sender_id: {
            type: DataTypes.INTEGER,
            references: {
                model: "users",
                key: "id",
            },
        },
        message: {
            type: DataTypes.STRING(255),
        },
        isTranscribed: {
            type: DataTypes.BOOLEAN,
            defaultValue: false,
        },
    });

    Message.belongsTo(sequelize.models.conversation, { foreignKey: "conversation_id", as: "conversation" });
    Message.belongsTo(sequelize.models.user, { foreignKey: "sender_id", as: "user" });

    Message.hasMany(sequelize.models.translation, { foreignKey: "message_id", as: "translations", sourceKey: "id" });

    return Message;
};
